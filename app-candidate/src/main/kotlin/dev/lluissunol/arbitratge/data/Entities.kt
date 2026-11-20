package dev.lluissunol.arbitratge.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla `session` (03 §4.4). La unicidad de la sesión activa se logra con [activeSlot]: vale 1 solo en
 * `IN_PROGRESS` y es NULL en el resto; el índice único permite muchos NULL y un único 1.
 */
@Entity(
    tableName = "session",
    indices = [Index(value = ["active_slot"], unique = true), Index(value = ["state", "finished_wall"])],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val state: String,
    @ColumnInfo(name = "active_slot") val activeSlot: Int?,
    val cid: String,
    @ColumnInfo(name = "candidate_name") val candidateName: String,
    @ColumnInfo(name = "bank_version") val bankVersion: String,
    @ColumnInfo(name = "exam_version") val examVersion: String,
    val rules: String,
    val snapshot: String,
    @ColumnInfo(name = "current_index") val currentIndex: Int,
    @ColumnInfo(name = "limit_ms") val limitMs: Long,
    @ColumnInfo(name = "remaining_ms_saved") val remainingMsSaved: Long,
    @ColumnInfo(name = "anchor_mono") val anchorMono: Long,
    @ColumnInfo(name = "anchor_boot") val anchorBoot: Int?,
    /** Indicio de reinicio cuando BOOT_COUNT no es fiable (03 §8.3); nunca decide el tiempo restante. */
    @ColumnInfo(name = "anchor_wall_ms") val anchorWallMs: Long,
    @ColumnInfo(name = "start_wall") val startWall: Long,
    @ColumnInfo(name = "finished_wall") val finishedWall: Long?,
    @ColumnInfo(name = "finish_reason") val finishReason: String?,
    @ColumnInfo(name = "correct_count") val correctCount: Int?,
)

@Entity(
    tableName = "answer",
    primaryKeys = ["session_id", "q_index"],
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["session_id"], onDelete = ForeignKey.CASCADE)],
)
data class AnswerEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "q_index") val qIndex: Int,
    /** JSON array de ids, en el orden mostrado. */
    @ColumnInfo(name = "selected_ids") val selectedIds: String,
    @ColumnInfo(name = "remaining_at_tap_ms") val remainingAtTapMs: Long,
)

@Entity(
    tableName = "credential",
    indices = [Index(value = ["session_id"], unique = true)],
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["session_id"], onDelete = ForeignKey.CASCADE)],
)
data class CredentialEntity(
    @PrimaryKey val cid: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val token: String,
    val name: String,
    val iat: String,
    val kid: String,
    val exv: String,
    val bv: String?,
    @ColumnInfo(name = "pdf_exported") val pdfExported: Boolean,
)
