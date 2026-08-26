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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.Gender
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
        Index(name = "idx_l0_generation_executes_domain_id", columnList = "domain_id"),
        Index(name = "idx_l0_generation_executes_lie_id", columnList = "lie_id"),
        Index(name = "idx_l0_generation_executes_wound_timing_id", columnList = "wound_timing_id"),
        Index(name = "idx_l0_generation_executes_wound_place_id", columnList = "wound_place_id"),
        Index(name = "idx_l0_generation_executes_wound_agent_id", columnList = "wound_agent_id"),
        Index(name = "idx_l0_generation_executes_wound_form_id", columnList = "wound_form_id"),
        Index(name = "idx_l0_generation_executes_wound_duration_id", columnList = "wound_duration_id"),
        Index(name = "idx_l0_generation_executes_openness_stage_id", columnList = "openness_stage_id"),
        Index(name = "idx_l0_generation_executes_conscientiousness_stage_id", columnList = "conscientiousness_stage_id"),
        Index(name = "idx_l0_generation_executes_extraversion_stage_id", columnList = "extraversion_stage_id"),
        Index(name = "idx_l0_generation_executes_agreeableness_stage_id", columnList = "agreeableness_stage_id"),
        Index(name = "idx_l0_generation_executes_neuroticism_stage_id", columnList = "neuroticism_stage_id"),
        Index(name = "idx_l0_generation_executes_attachment_style_id", columnList = "attachment_style_id"),
        Index(name = "idx_l0_generation_executes_emotional_expression_id", columnList = "emotional_expression_id"),
        Index(name = "idx_l0_generation_executes_avoidance_style_id", columnList = "avoidance_style_id"),
        Index(name = "idx_l0_generation_executes_speech_archetype_id", columnList = "speech_archetype_id"),
        Index(name = "idx_l0_generation_executes_speech_register_id", columnList = "speech_register_id"),
        Index(name = "idx_l0_generation_executes_character_id", columnList = "character_id"),
    ],
)
class L0GenerationExecute(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_template_ref_id", nullable = false)
    val promptTemplateRef: PromptTemplateRef,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "domain_id", nullable = true)
    val domain: DomainEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "lie_id", nullable = true)
    val lie: LieEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "wound_timing_id", nullable = true)
    val woundTiming: WoundTimingEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "wound_place_id", nullable = true)
    val woundPlace: WoundPlaceEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "wound_agent_id", nullable = true)
    val woundAgent: WoundAgentEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "wound_form_id", nullable = true)
    val woundForm: WoundFormEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "wound_duration_id", nullable = true)
    val woundDuration: WoundDurationEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "openness_stage_id", nullable = true)
    val opennessStage: PersonalityTraitStageEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "conscientiousness_stage_id", nullable = true)
    val conscientiousnessStage: PersonalityTraitStageEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "extraversion_stage_id", nullable = true)
    val extraversionStage: PersonalityTraitStageEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "agreeableness_stage_id", nullable = true)
    val agreeablenessStage: PersonalityTraitStageEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "neuroticism_stage_id", nullable = true)
    val neuroticismStage: PersonalityTraitStageEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "attachment_style_id", nullable = true)
    val attachmentStyle: AttachmentStyleEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "emotional_expression_id", nullable = true)
    val emotionalExpression: EmotionalExpressionEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "avoidance_style_id", nullable = true)
    val avoidanceStyle: AvoidanceStyleEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "speech_archetype_id", nullable = true)
    val speechArchetype: SpeechArchetypeEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "speech_register_id", nullable = true)
    val speechRegister: SpeechRegisterEntity? = null,

    @Lob
    @Column(nullable = true)
    val description: String? = null,

    @Column(nullable = true, length = 100)
    val name: String? = null,

    @Column(nullable = true)
    val age: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 10)
    val gender: Gender? = null,

    @Column(nullable = true)
    val scale: Int? = null,

    @Column(nullable = true)
    val emotion: Int? = null,

    @Lob
    @Column(name = "extra_constraints", nullable = true)
    val extraConstraints: String? = null,

    /**
     * 이 실행에서 프롬프트에 알려준 기존 캐릭터 목록.
     *
     * 자동으로 붙는 최근 캐릭터는 시간이 지나면 달라지므로, 나중에 다시 계산하지 않고 요청 시점의 값을 남긴다.
     * 알려준 캐릭터가 없으면 null이다.
     */
    @Lob
    @Column(name = "existing_characters_note", nullable = true)
    val existingCharactersNote: String? = null,

    @Lob
    @Column(name = "prompt_ref_snapshot_json")
    val promptRefSnapshotJson: String? = null,

    @Lob
    @Column(name = "user_input_json", nullable = false)
    var userInputJson: String,

    @Lob
    @Column(name = "llm_output")
    var llmOutput: String? = null,

    /** 이 실행으로 생성된 Character. 생성 완료 전에는 null이다. */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "character_id", nullable = true, unique = true)
    var character: Character? = null,

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
