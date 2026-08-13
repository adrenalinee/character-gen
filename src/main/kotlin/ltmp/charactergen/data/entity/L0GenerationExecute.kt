package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.LongBaseEntity
import java.time.OffsetDateTime

@Entity
@Table(
    name = "l0_generation_executes",
    indexes = [
        Index(name = "idx_l0_generation_executes_status", columnList = "status"),
        Index(name = "idx_l0_generation_executes_created_at", columnList = "created_at"),
        Index(
            name = "idx_l0_generation_executes_prompt_template_ref_id",
            columnList = "prompt_template_ref_id",
        ),
    ],
)
class L0GenerationExecute(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_ref_id", nullable = false)
    val promptTemplateRef: PromptTemplateRef,

    @Lob
    @Column(name = "prompt_ref_snapshot_json")
    val promptRefSnapshotJson: String? = null,

    @Lob
    @Column(name = "user_input_json", nullable = false)
    var userInputJson: String,

    @Lob
    @Column(name = "llm_output")
    var llmOutput: String? = null,

    /** 이 실행으로 생성된 Character의 식별자. 생성 완료 전에는 null이다. */
    @Column(name = "character_id")
    var characterId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: L0GenerationExecuteStatus,

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
