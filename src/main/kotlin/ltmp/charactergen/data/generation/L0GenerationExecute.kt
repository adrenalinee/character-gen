package ltmp.charactergen.data.generation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity
import java.time.OffsetDateTime

@Entity
@Table(
    name = "l0_generation_executes",
    indexes = [
        Index(name = "idx_l0_generation_executes_status", columnList = "status"),
        Index(name = "idx_l0_generation_executes_created_at", columnList = "created_at"),
    ],
)
class L0GenerationExecute(
    @Lob
    @Column(name = "user_input_json", nullable = false)
    var userInputJson: String = "",

    @Lob
    @Column(name = "llm_output")
    var llmOutput: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: L0GenerationExecuteStatus = L0GenerationExecuteStatus.PREPARING,

    @Lob
    @Column(name = "error_trace")
    var errorTrace: String? = null,

    @Lob
    @Column
    var message: String? = null,

    /** LLM 호출에 걸린 시간(밀리초). */
    @Column
    var elapsed: Long? = null,

    @Column(name = "completed_at")
    var completedAt: OffsetDateTime? = null,
) : LongBaseEntity()
