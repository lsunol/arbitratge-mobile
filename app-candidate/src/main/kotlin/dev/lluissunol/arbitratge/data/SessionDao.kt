package dev.lluissunol.arbitratge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class FinishRow(val reason: String, val correct: Int, val finishedWall: Long)

@Dao
abstract class SessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSession(s: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAnswer(a: AnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCredentialIgnore(c: CredentialEntity): Long

    @Query("SELECT * FROM session WHERE id = :id")
    abstract suspend fun session(id: String): SessionEntity?

    @Query("SELECT * FROM session WHERE state = 'IN_PROGRESS' LIMIT 1")
    abstract suspend fun active(): SessionEntity?

    @Query("SELECT * FROM session WHERE state = 'FINISHED' ORDER BY rowid DESC LIMIT 1")
    abstract suspend fun latestFinished(): SessionEntity?

    @Query(
        "SELECT s.* FROM session s LEFT JOIN credential c ON c.session_id = s.id " +
            "WHERE s.state = 'FINISHED' AND s.kind = 'EXAM' AND c.cid IS NULL ORDER BY s.rowid",
    )
    abstract suspend fun finishedWithoutCredential(): List<SessionEntity>

    @Query("SELECT id FROM session WHERE state IN ('IN_PROGRESS', 'FINISHED', 'ABANDONED')")
    abstract suspend fun readableCandidateIds(): List<String>

    @Query("UPDATE session SET state = 'UNREADABLE', active_slot = NULL WHERE id = :id")
    abstract suspend fun markUnreadable(id: String): Int

    @Query("SELECT * FROM answer WHERE session_id = :id ORDER BY q_index")
    abstract suspend fun answers(id: String): List<AnswerEntity>

    @Query("SELECT * FROM answer WHERE session_id = :id AND q_index = :i")
    abstract suspend fun answer(id: String, i: Int): AnswerEntity?

    @Query(
        "UPDATE session SET current_index = :currentIndex, remaining_ms_saved = :remaining, anchor_mono = :mono, " +
            "anchor_boot = :boot, anchor_wall_ms = :wall WHERE id = :id AND state = 'IN_PROGRESS'",
    )
    abstract suspend fun updateProgress(id: String, currentIndex: Int, remaining: Long, mono: Long, boot: Int?, wall: Long): Int

    @Query(
        "UPDATE session SET remaining_ms_saved = :remaining, anchor_mono = :mono, anchor_boot = :boot, " +
            "anchor_wall_ms = :wall WHERE id = :id AND state = 'IN_PROGRESS'",
    )
    abstract suspend fun updateClock(id: String, remaining: Long, mono: Long, boot: Int?, wall: Long): Int

    @Query(
        "UPDATE session SET state = 'FINISHED', active_slot = NULL, finish_reason = :reason, correct_count = :correct, " +
            "finished_wall = :finishedWall WHERE id = :id AND state = 'IN_PROGRESS'",
    )
    abstract suspend fun markFinished(id: String, reason: String, correct: Int, finishedWall: Long): Int

    @Query("UPDATE session SET state = 'ABANDONED', active_slot = NULL, finish_reason = 'ABANDONED' WHERE id = :id AND state = 'IN_PROGRESS'")
    abstract suspend fun markAbandoned(id: String): Int

    @Query("SELECT * FROM credential WHERE session_id = :id")
    abstract suspend fun credentialForSession(id: String): CredentialEntity?

    @Query("SELECT c.* FROM credential c JOIN session s ON s.id = c.session_id ORDER BY s.rowid DESC LIMIT 1")
    abstract suspend fun latestCredential(): CredentialEntity?

    // ---- Transacciones: cada una es atómica (03 §4.3, §4.4) ----

    @Transaction
    open suspend fun createTx(s: SessionEntity) {
        insertSession(s)
    }

    @Transaction
    open suspend fun commitAnswerTx(
        a: AnswerEntity,
        nextIndex: Int,
        remaining: Long,
        mono: Long,
        boot: Int?,
        wall: Long,
        finish: FinishRow?,
    ) {
        val current = session(a.sessionId) ?: throw SessionNotActiveException("sessió inexistent")
        if (current.state != "IN_PROGRESS") throw SessionNotActiveException("sessió no activa: ${current.state}")
        if (insertAnswer(a) == -1L) {
            val existing = answer(a.sessionId, a.qIndex)
            if (existing != a) throw IllegalStateException("resposta ja desada amb altre contingut: ${a.qIndex}")
        }
        updateProgress(a.sessionId, nextIndex, remaining, mono, boot, wall)
        if (finish != null) markFinished(a.sessionId, finish.reason, finish.correct, finish.finishedWall)
    }

    @Transaction
    open suspend fun finishTx(id: String, remaining: Long, mono: Long, boot: Int?, wall: Long, finish: FinishRow) {
        val current = session(id) ?: throw SessionNotActiveException("sessió inexistent")
        if (current.state == "FINISHED") return
        if (current.state != "IN_PROGRESS") throw SessionNotActiveException("sessió no activa: ${current.state}")
        updateClock(id, remaining, mono, boot, wall)
        markFinished(id, finish.reason, finish.correct, finish.finishedWall)
    }

    @Transaction
    open suspend fun insertCredentialTx(c: CredentialEntity): CredentialEntity {
        val existing = credentialForSession(c.sessionId)
        if (existing != null) return existing
        insertCredentialIgnore(c)
        return credentialForSession(c.sessionId) ?: throw IllegalStateException("credencial no desada")
    }
}
