package dev.lluissunol.arbitratge.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamClockModel
import dev.lluissunol.arbitratge.exam.ExamEngine
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.SeededRng
import dev.lluissunol.arbitratge.exam.SessionState
import dev.lluissunol.arbitratge.support.FakeClock
import dev.lluissunol.arbitratge.support.FakePrefs
import dev.lluissunol.arbitratge.support.SwitchableIssuing
import dev.lluissunol.arbitratge.support.TestEnv
import dev.lluissunol.arbitratge.ui.CredentialPhase
import dev.lluissunol.arbitratge.ui.ExamViewModel
import dev.lluissunol.arbitratge.ui.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.lifecycle.viewModelScope
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Room real (SQLite de Robolectric): transacciones, unicidad, fallos y recuperación tras "muerte del proceso". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPersistenceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var db: AppDatabase? = null

    private fun openDb(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "test-arbitratge.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .allowMainThreadQueries()
            .build().also { db = it }

    @Before fun clean() {
        context.deleteDatabase("test-arbitratge.db")
    }

    @After fun close() {
        db?.close()
        context.deleteDatabase("test-arbitratge.db")
        Dispatchers.resetMain()
    }

    private fun newSession(id: String, cid: String = "7f3a92c1-4b5d-4e6f-8a9b-%012d".format(id.hashCode().toLong().and(0xFFFFFF))): StoredSession {
        val bank = TestEnv.bank
        val session = ExamEngine.start(bank, bank.rules, SeededRng(1))
        return StoredSession(
            id = id, kind = SessionKind.EXAM, state = SessionState.IN_PROGRESS, cid = cid,
            candidateName = "Maria Puig Vila", session = session, currentIndex = 0,
            clock = ExamClockModel.initial(bank.rules.limitMs, 1_000L, null, 1_800_000_000_000L), startWall = 1_800_000_000_000L,
        )
    }

    @Test fun roundTripKeepsEverythingIncludingNullBootAndAnchorWall() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        val s = newSession("s1")
        store.create(s)
        val back = assertNotNull(store.active())
        assertEquals(s, back)
        assertNull(back.clock.anchorBoot)
        assertEquals(1_800_000_000_000L, back.clock.anchorWallMs)
    }

    @Test fun onlyOneInProgressSession() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        store.create(newSession("s1"))
        assertFailsWith<StorageException> { store.create(newSession("s2", "7f3a92c1-4b5d-4e6f-8a9b-000000000002")) }
        assertEquals("s1", store.active()?.id)
        // al terminar, queda libre la plaza
        val clock = ClockState(300_000, 0, 5, null, 6)
        store.finish("s1", clock, Finish(FinishReason.TIMED_OUT, 0, 7))
        store.create(newSession("s3", "7f3a92c1-4b5d-4e6f-8a9b-000000000003"))
        assertEquals("s3", store.active()?.id)
    }

    @Test fun commitAnswerIsOneTransactionAndIdempotent() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        val s = newSession("s1")
        store.create(s)
        val q0 = s.session.snapshot.questions[0]
        val ans = AnswerRecord(0, listOf(q0.options.first().id), 200_000)
        val clock = ClockState(300_000, 200_000, 50_000, 9, 1_800_000_100_000L)
        store.commitAnswer("s1", ans, clock, null)
        store.commitAnswer("s1", ans, clock, null) // doble toque/reintento: misma fila
        val back = store.session("s1")!!
        assertEquals(listOf(ans), back.session.answers)
        assertEquals(1, back.currentIndex)
        assertEquals(clock, back.clock)
        // respuesta distinta para el mismo (session, q_index): rechazada, el original no cambia
        assertFailsWith<IllegalStateException> { store.commitAnswer("s1", ans.copy(selectedIds = emptyList()), clock, null) }
        assertEquals(listOf(ans), store.session("s1")!!.session.answers)
    }

    @Test fun commitOnInactiveSessionWritesNothing() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        store.create(newSession("s1"))
        store.finish("s1", ClockState(300_000, 0, 5, null, 6), Finish(FinishReason.TIMED_OUT, 0, 7))
        val ans = AnswerRecord(0, listOf("a"), 10)
        assertFailsWith<SessionNotActiveException> { store.commitAnswer("s1", ans, ClockState(300_000, 1, 5, null, 6), null) }
        assertTrue(store.session("s1")!!.session.answers.isEmpty(), "la transacción revierte: ninguna fila de respuesta")
        assertFailsWith<SessionNotActiveException> { store.commitAnswer("nope", ans, ClockState(300_000, 1, 5, null, 6), null) }
    }

    @Test fun lastAnswerFinishesInTheSameTransaction() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        val s = newSession("s1")
        store.create(s)
        s.session.snapshot.questions.forEachIndexed { i, q ->
            val last = i == s.session.total - 1
            store.commitAnswer(
                "s1", AnswerRecord(i, q.correctIds, 100_000), ClockState(300_000, 100_000, 5, null, 6),
                if (last) Finish(FinishReason.COMPLETED, s.session.total, 99) else null,
            )
        }
        val back = store.session("s1")!!
        assertEquals(SessionState.FINISHED, back.state)
        assertEquals(FinishReason.COMPLETED, back.finishReason)
        assertEquals(s.session.total, back.correctCount)
        assertNull(store.active())
        assertEquals("s1", store.latestFinished()?.id)
    }

    @Test fun abandonAndFinishAreIdempotentAndTerminal() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        store.create(newSession("s1"))
        store.abandon("s1"); store.abandon("s1")
        assertEquals(SessionState.ABANDONED, store.session("s1")!!.state)
        assertNull(store.active())
        assertNull(store.latestFinished())
        assertFailsWith<SessionNotActiveException> { store.finish("s1", ClockState(1, 0, 1, null, 1), Finish(FinishReason.TIMED_OUT, 0, 1)) }
    }

    @Test fun credentialInsertIsIdempotentPerSession() = runBlocking<Unit> {
        val store = RoomSessionStore(openDb().sessionDao())
        val s = newSession("s1")
        store.create(s)
        val c = StoredCredential(s.cid, "s1", "ARB1.aaa.bbb", "Maria Puig Vila", "2026-11-20", "dev", "sample-1", "0.0.0-sample")
        val first = store.insertCredential(c)
        val second = store.insertCredential(c.copy(token = "ARB1.otro.token"))
        assertEquals(first, second)
        assertEquals("ARB1.aaa.bbb", store.credentialFor("s1")!!.token)
    }

    @Test fun corruptStoredDataSurfacesAsStorageErrorAndStateErrorScreen() = runBlocking<Unit> {
        val database = openDb()
        val store = RoomSessionStore(database.sessionDao())
        store.create(newSession("s1"))
        database.openHelper.writableDatabase.execSQL("UPDATE session SET snapshot = '{corrupt'")
        assertFailsWith<StorageException> { store.active() }
        Dispatchers.setMain(Dispatchers.Default)
        val vm = ExamViewModel(TestEnv.deps(RoomSessionStore(database.sessionDao()), FakeClock(), SwitchableIssuing(TestEnv.realIssuing), FakePrefs()))
        withTimeout(20_000) { vm.stage.first { it == Stage.StateError } }
        vm.viewModelScope.cancel()
        database.openHelper.writableDatabase.execSQL("UPDATE session SET snapshot = '{}', state = 'ZZZ'")
        assertFailsWith<StorageException> { store.session("s1") }
    }

    @Test fun quarantineMarksUnreadableRowsTerminalAndUnblocksRecovery() = runBlocking<Unit> {
        val database = openDb()
        val store = RoomSessionStore(database.sessionDao())
        val s = newSession("s1")
        store.create(s)
        store.finish("s1", ClockState(300_000, 0, 5, null, 6), Finish(FinishReason.TIMED_OUT, 5, 7))
        store.create(newSession("s2", "7f3a92c1-4b5d-4e6f-8a9b-000000000002"))
        database.openHelper.writableDatabase.execSQL("UPDATE session SET snapshot = '{corrupt' WHERE id = 's1'")
        assertFailsWith<StorageException> { store.finishedWithoutCredential() }
        assertEquals(1, store.quarantineUnreadable())
        assertEquals(0, store.quarantineUnreadable(), "idempotente")
        assertTrue(store.finishedWithoutCredential().isEmpty())
        assertNull(store.latestFinished())
        assertEquals("s2", store.active()?.id, "la sesión legible no se toca")
        store.abandon("s2")
        store.create(newSession("s3", "7f3a92c1-4b5d-4e6f-8a9b-000000000003")) // se puede empezar de nuevo
    }

    @Test fun inconsistentActiveSlotRowsDoNotBlockNewSessions() = runBlocking<Unit> {
        val database = openDb()
        val store = RoomSessionStore(database.sessionDao())
        store.create(newSession("s1"))
        val sql = database.openHelper.writableDatabase
        sql.execSQL("UPDATE session SET state = 'ZZZ' WHERE id = 's1'") // activa (slot=1) pero estado desconocido
        assertNull(store.active())
        store.create(newSession("s2", "7f3a92c1-4b5d-4e6f-8a9b-000000000002")) // antes: violación de unicidad
        assertEquals("s2", store.active()?.id)
        fun q(id: String) = sql.query("SELECT state, active_slot FROM session WHERE id = '$id'").use { it.moveToFirst(); it.getString(0) to it.isNull(1) }
        assertEquals("UNREADABLE" to true, q("s1"))
        // estado terminal con plaza activa colgada, y la vía de recuperación explícita
        store.abandon("s2")
        sql.execSQL("UPDATE session SET active_slot = 1 WHERE id = 's2'")
        assertEquals(0, store.quarantineUnreadable(), "un estado terminal conocido no se pone en cuarentena")
        assertEquals("ABANDONED" to true, q("s2"))
        store.create(newSession("s3", "7f3a92c1-4b5d-4e6f-8a9b-000000000003"))
        assertEquals("s3", store.active()?.id)
    }

    @Test fun finishedApprovedSessionWithDanglingSlotStillAppearsForIssuance() = runBlocking<Unit> {
        val database = openDb()
        val store = RoomSessionStore(database.sessionDao())
        val s = newSession("s1")
        store.create(s)
        s.session.snapshot.questions.forEachIndexed { i, q ->
            store.commitAnswer(
                "s1", AnswerRecord(i, q.correctIds, 100_000), ClockState(300_000, 100_000, 5, null, 6),
                if (i == s.session.total - 1) Finish(FinishReason.COMPLETED, s.session.total, 99) else null,
            )
        }
        database.openHelper.writableDatabase.execSQL("UPDATE session SET active_slot = 1 WHERE id = 's1'")
        store.create(newSession("s2", "7f3a92c1-4b5d-4e6f-8a9b-000000000002")) // no queda bloqueado
        val pending = store.finishedWithoutCredential()
        assertEquals(listOf("s1"), pending.map { it.id })
        assertEquals(SessionState.FINISHED, pending.single().state)
    }

    // ---- muerte del proceso: todo se recompone desde la base de datos ----

    @Test fun processDeathRecoveryOverRealDatabase() = runBlocking<Unit> {
        Dispatchers.setMain(Dispatchers.Default)
        val clock = FakeClock()
        val issuing = SwitchableIssuing(TestEnv.realIssuing)
        var database = openDb()
        var vm = ExamViewModel(TestEnv.deps(RoomSessionStore(database.sessionDao()), clock, issuing, FakePrefs()))
        suspend fun <T : Stage> await(klass: Class<T>): T = withTimeout(20_000) { @Suppress("UNCHECKED_CAST") (vm.stage.first { klass.isInstance(it) } as T) }

        await(Stage.Home::class.java)
        vm.onStartExam(); await(Stage.Name::class.java)
        vm.onNameChange("Maria Puig Vila"); vm.onNameContinue()
        vm.onBeginExam()
        var q = await(Stage.Question::class.java)
        // responde 2 preguntas bien
        repeat(2) {
            val cur = vm.stage.value as Stage.Question
            cur.question.correctIds.forEach(vm::onSelect)
            val idx = cur.index
            vm.onNext()
            withTimeout(20_000) { vm.stage.first { it is Stage.Question && it.index == idx + 1 } }
        }
        vm.viewModelScope.cancel()
        database.close() // "muerte del proceso"
        clock.advance(30_000)

        database = openDb()
        val store2 = RoomSessionStore(database.sessionDao())
        vm = ExamViewModel(TestEnv.deps(store2, clock, issuing, FakePrefs()))
        q = await(Stage.Question::class.java)
        assertEquals(2, q.index)
        assertEquals(2, store2.active()!!.session.answers.size)
        // termina el examen
        repeat(3) {
            val cur = vm.stage.value as Stage.Question
            cur.question.correctIds.forEach(vm::onSelect)
            vm.onNext()
            if (cur.isLast) { withTimeout(20_000) { vm.stage.first { (it as? Stage.Question)?.dialog != null } }; vm.onDialogConfirm() }
            else withTimeout(20_000) { vm.stage.first { it is Stage.Question && it.index == cur.index + 1 } }
        }
        val result = await(Stage.Result::class.java)
        val phase = withTimeout(20_000) { (vm.stage.first { it is Stage.Result && it.credential is CredentialPhase.Ready } as Stage.Result).credential }
        val token = (phase as CredentialPhase.Ready).credential.token
        assertTrue(result.result.passed)
        vm.viewModelScope.cancel()
        database.close()

        // reabre: Home, con la misma credencial (token exacto, sin re-emitir)
        database = openDb()
        val store3 = RoomSessionStore(database.sessionDao())
        vm = ExamViewModel(TestEnv.deps(store3, clock, issuing, FakePrefs()))
        val home = await(Stage.Home::class.java)
        assertEquals(token, home.credential?.token)
        assertEquals(1, issuing.cids.size)
        vm.viewModelScope.cancel()
    }
}
