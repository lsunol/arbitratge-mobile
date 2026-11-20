package dev.lluissunol.arbitratge.data

import android.database.SQLException
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamJson
import dev.lluissunol.arbitratge.exam.ExamRules
import dev.lluissunol.arbitratge.exam.ExamSession
import dev.lluissunol.arbitratge.exam.ExamSnapshot
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.SessionState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Implementación Room de [SessionStore]. Los errores de SQLite se envuelven en [StorageException]. */
class RoomSessionStore(private val dao: SessionDao) : SessionStore {

    private val idsSerializer = ListSerializer(String.serializer())

    private suspend fun <T> guard(block: suspend () -> T): T =
        try {
            block()
        } catch (e: SQLException) {
            throw StorageException("error d'emmagatzematge", e)
        } catch (e: SerializationException) { // dades desades il·legibles (corrupció): mateix error recuperable
            throw StorageException("dades desades il·legibles", e)
        } catch (e: IllegalArgumentException) {
            throw StorageException("dades desades il·legibles", e)
        }

    override suspend fun active(): StoredSession? = guard { dao.active()?.let { load(it) } }
    override suspend fun latestFinished(): StoredSession? = guard { dao.latestFinished()?.let { load(it) } }
    override suspend fun finishedWithoutCredential(): List<StoredSession> =
        guard { dao.finishedWithoutCredential().map { load(it) } }

    override suspend fun quarantineUnreadable(): Int = guard {
        var n = 0
        for (id in dao.readableCandidateIds()) {
            val ok = try {
                dao.session(id)?.let { load(it) }
                true
            } catch (e: SerializationException) {
                false
            } catch (e: IllegalArgumentException) {
                false
            }
            if (!ok) { dao.markUnreadable(id); n++ }
        }
        n
    }

    override suspend fun session(id: String): StoredSession? = guard { dao.session(id)?.let { load(it) } }

    override suspend fun create(session: StoredSession) = guard {
        require(session.state == SessionState.IN_PROGRESS) { "només es creen sessions IN_PROGRESS" }
        dao.createTx(session.toEntity())
    }

    override suspend fun commitAnswer(sessionId: String, answer: AnswerRecord, clock: ClockState, finish: Finish?) = guard {
        dao.commitAnswerTx(
            a = AnswerEntity(
                sessionId, answer.index, ExamJson.encodeToString(idsSerializer, answer.selectedIds), answer.remainingAtTapMs,
            ),
            nextIndex = answer.index + 1,
            remaining = clock.remainingMsSaved,
            mono = clock.anchorMono,
            boot = clock.anchorBoot,
            wall = clock.anchorWallMs,
            finish = finish?.let { FinishRow(it.reason.name, it.correctCount, it.finishedWall) },
        )
    }

    override suspend fun saveClock(sessionId: String, clock: ClockState) = guard {
        dao.updateClock(sessionId, clock.remainingMsSaved, clock.anchorMono, clock.anchorBoot, clock.anchorWallMs)
        Unit
    }

    override suspend fun finish(sessionId: String, clock: ClockState, finish: Finish) = guard {
        dao.finishTx(
            sessionId, clock.remainingMsSaved, clock.anchorMono, clock.anchorBoot, clock.anchorWallMs,
            FinishRow(finish.reason.name, finish.correctCount, finish.finishedWall),
        )
    }

    override suspend fun abandon(sessionId: String) = guard {
        dao.markAbandoned(sessionId)
        Unit
    }

    override suspend fun credentialFor(sessionId: String): StoredCredential? =
        guard { dao.credentialForSession(sessionId)?.toModel() }

    override suspend fun latestCredential(): StoredCredential? = guard { dao.latestCredential()?.toModel() }

    override suspend fun insertCredential(credential: StoredCredential): StoredCredential =
        guard { dao.insertCredentialTx(credential.toEntity()).toModel() }

    private suspend fun load(e: SessionEntity): StoredSession {
        val answers = dao.answers(e.id).map {
            AnswerRecord(it.qIndex, ExamJson.decodeFromString(idsSerializer, it.selectedIds), it.remainingAtTapMs)
        }
        val snapshot = ExamJson.decodeFromString(ExamSnapshot.serializer(), e.snapshot)
        return StoredSession(
            id = e.id,
            kind = SessionKind.valueOf(e.kind),
            state = SessionState.valueOf(e.state),
            cid = e.cid,
            candidateName = e.candidateName,
            session = ExamSession(snapshot, answers),
            currentIndex = e.currentIndex,
            clock = ClockState(e.limitMs, e.remainingMsSaved, e.anchorMono, e.anchorBoot, e.anchorWallMs),
            startWall = e.startWall,
            finishedWall = e.finishedWall,
            finishReason = e.finishReason?.let { FinishReason.valueOf(it) },
            correctCount = e.correctCount,
        )
    }

    private fun StoredSession.toEntity() = SessionEntity(
        id = id,
        kind = kind.name,
        state = state.name,
        activeSlot = if (state == SessionState.IN_PROGRESS) 1 else null,
        cid = cid,
        candidateName = candidateName,
        bankVersion = bankVersion,
        examVersion = examVersion,
        rules = ExamJson.encodeToString(ExamRules.serializer(), session.snapshot.rules),
        snapshot = ExamJson.encodeToString(ExamSnapshot.serializer(), session.snapshot),
        currentIndex = currentIndex,
        limitMs = clock.limitMs,
        remainingMsSaved = clock.remainingMsSaved,
        anchorMono = clock.anchorMono,
        anchorBoot = clock.anchorBoot,
        anchorWallMs = clock.anchorWallMs,
        startWall = startWall,
        finishedWall = finishedWall,
        finishReason = finishReason?.name,
        correctCount = correctCount,
    )

    private fun StoredCredential.toEntity() = CredentialEntity(cid, sessionId, token, name, iat, kid, exv, bv, pdfExported)
    private fun CredentialEntity.toModel() = StoredCredential(cid, sessionId, token, name, iat, kid, exv, bv, pdfExported)
}
