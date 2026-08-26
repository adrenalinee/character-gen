package ltmp.charactergen.service

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.api.L0GenerationExecuteDto
import ltmp.charactergen.api.L0GenerationExecuteSimpleDto
import ltmp.charactergen.api.axis.L0AxisBalanceCheckDto
import ltmp.charactergen.api.axis.L0AxisBalanceCheckRequest
import ltmp.charactergen.api.axis.toCheckDto
import ltmp.charactergen.api.toDto
import ltmp.charactergen.api.toSimpleDto
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import ltmp.charactergen.data.repo.WoundAgentRepository
import ltmp.charactergen.data.repo.WoundDurationRepository
import ltmp.charactergen.data.repo.WoundFormRepository
import ltmp.charactergen.data.repo.WoundPlaceRepository
import ltmp.charactergen.data.repo.WoundTimingRepository
import ltmp.charactergen.service.generation.PromptRefInputSnapshot
import ltmp.charactergen.service.generation.PromptRefSnapshot
import ltmp.charactergen.service.generation.L0GenerationTaskDispatcher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class L0CharacterGenerationService(
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val defaultPromptTemplateService: DefaultPromptTemplateService,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val woundTimingRepository: WoundTimingRepository,
    private val woundPlaceRepository: WoundPlaceRepository,
    private val woundAgentRepository: WoundAgentRepository,
    private val woundFormRepository: WoundFormRepository,
    private val woundDurationRepository: WoundDurationRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val characterRepository: CharacterRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
    private val axisBalanceService: L0AxisBalanceService,
    private val promptTemplateRefService: PromptTemplateRefService,
    private val objectMapper: ObjectMapper,
    private val taskDispatcher: L0GenerationTaskDispatcher,
) {
    @Transactional(readOnly = true)
    fun searchGenerations(
        refId: Long?,
        status: L0GenerationExecuteStatus?,
        pageable: Pageable,
    ): Page<L0GenerationExecuteSimpleDto> =
        generationExecuteRepository.search(refId, status, pageable).map(L0GenerationExecute::toSimpleDto)

    @Transactional(readOnly = true)
    fun getGeneration(generationId: Long): L0GenerationExecuteDto =
        (generationExecuteRepository.findOneWithDetailsById(generationId)
            ?: throw ResourceNotFoundException("L0GenerationExecute not found: $generationId"))
            .toDto()

    @Transactional
    fun requestGeneration(request: L0CharacterGenerationRequest): L0GenerationExecuteDto {
        val ref = resolveGenerationRef(request.refId)
        promptTemplateRefService.validateExecutable(ref)

        val snapshotJson = if (ref.type == PromptTemplateRefType.BRANCH) {
            objectMapper.writeValueAsString(
                PromptRefSnapshot(
                    instructions = ref.instructions,
                    inputs = ref.inputs.map { PromptRefInputSnapshot(role = it.role, inputText = it.inputText) },
                ),
            )
        } else {
            null
        }
        val domain = request.domainId?.let { id ->
            domainRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Domain not found: $id") }
        }
        val lie = request.lieId?.let { id ->
            lieRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("Lie not found: $id") }
        }
        val woundTiming = request.woundTimingId?.let { id ->
            woundTimingRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("WoundTiming not found: $id") }
        }
        val woundPlace = request.woundPlaceId?.let { id ->
            woundPlaceRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("WoundPlace not found: $id") }
        }
        val woundAgent = request.woundAgentId?.let { id ->
            woundAgentRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("WoundAgent not found: $id") }
        }
        val woundForm = request.woundFormId?.let { id ->
            woundFormRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("WoundForm not found: $id") }
        }
        val woundDuration = request.woundDurationId?.let { id ->
            woundDurationRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("WoundDuration not found: $id") }
        }
        val opennessStage = resolveTraitStage(request.opennessStageId, BigFiveTrait.OPENNESS)
        val conscientiousnessStage = resolveTraitStage(
            request.conscientiousnessStageId,
            BigFiveTrait.CONSCIENTIOUSNESS,
        )
        val extraversionStage = resolveTraitStage(request.extraversionStageId, BigFiveTrait.EXTRAVERSION)
        val agreeablenessStage = resolveTraitStage(request.agreeablenessStageId, BigFiveTrait.AGREEABLENESS)
        val neuroticismStage = resolveTraitStage(request.neuroticismStageId, BigFiveTrait.NEUROTICISM)
        validateExtremeTraitStages(
            listOf(
                opennessStage,
                conscientiousnessStage,
                extraversionStage,
                agreeablenessStage,
                neuroticismStage,
            ),
        )
        val attachmentStyle = attachmentStyleRepository.findById(request.attachmentStyleId)
            .orElseThrow { ResourceNotFoundException("AttachmentStyle not found: ${request.attachmentStyleId}") }
        val emotionalExpression = emotionalExpressionRepository.findById(request.emotionalExpressionId)
            .orElseThrow {
                ResourceNotFoundException("EmotionalExpression not found: ${request.emotionalExpressionId}")
            }
        val avoidanceStyle = avoidanceStyleRepository.findById(request.avoidanceStyleId)
            .orElseThrow { ResourceNotFoundException("AvoidanceStyle not found: ${request.avoidanceStyleId}") }
        val speechArchetype = speechArchetypeRepository.findById(request.speechArchetypeId)
            .orElseThrow { ResourceNotFoundException("SpeechArchetype not found: ${request.speechArchetypeId}") }
        val speechRegister = request.speechRegisterId?.let { id ->
            speechRegisterRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("SpeechRegister not found: $id") }
        }
        validateAxisBalance(
            axisBalanceService.evaluate(
                opennessStage = opennessStage,
                conscientiousnessStage = conscientiousnessStage,
                extraversionStage = extraversionStage,
                agreeablenessStage = agreeablenessStage,
                neuroticismStage = neuroticismStage,
                attachmentStyle = attachmentStyle,
                emotionalExpression = emotionalExpression,
                avoidanceStyle = avoidanceStyle,
                speechArchetype = speechArchetype,
            ),
        )

        val execute = L0GenerationExecute(
            promptTemplateRef = ref,
            domain = domain,
            lie = lie,
            woundTiming = woundTiming,
            woundPlace = woundPlace,
            woundAgent = woundAgent,
            woundForm = woundForm,
            woundDuration = woundDuration,
            opennessStage = opennessStage,
            conscientiousnessStage = conscientiousnessStage,
            extraversionStage = extraversionStage,
            agreeablenessStage = agreeablenessStage,
            neuroticismStage = neuroticismStage,
            attachmentStyle = attachmentStyle,
            emotionalExpression = emotionalExpression,
            avoidanceStyle = avoidanceStyle,
            speechArchetype = speechArchetype,
            speechRegister = speechRegister,
            description = request.description,
            name = request.name,
            age = request.age,
            gender = request.gender,
            scale = request.scale,
            emotion = request.emotion,
            extraConstraints = request.extraConstraints,
            existingCharactersNote = existingCharactersNote(request.existingCharacterIds),
            promptRefSnapshotJson = snapshotJson,
            userInputJson = objectMapper.writeValueAsString(request),
            status = L0GenerationExecuteStatus.PREPARING,
        )
        val savedExecute = generationExecuteRepository.saveAndFlush(execute)
        taskDispatcher.dispatchAfterCommit(savedExecute.id)
        return savedExecute.toDto()
    }

    @Transactional(readOnly = true)
    fun checkAxisBalance(request: L0AxisBalanceCheckRequest): L0AxisBalanceCheckDto {
        val result = axisBalanceService.evaluate(
            opennessStage = findTraitStage(request.opennessStageId, BigFiveTrait.OPENNESS),
            conscientiousnessStage = findTraitStage(
                request.conscientiousnessStageId,
                BigFiveTrait.CONSCIENTIOUSNESS,
            ),
            extraversionStage = findTraitStage(request.extraversionStageId, BigFiveTrait.EXTRAVERSION),
            agreeablenessStage = findTraitStage(request.agreeablenessStageId, BigFiveTrait.AGREEABLENESS),
            neuroticismStage = findTraitStage(request.neuroticismStageId, BigFiveTrait.NEUROTICISM),
            attachmentStyle = request.attachmentStyleId?.let { id ->
                attachmentStyleRepository.findById(id)
                    .orElseThrow { ResourceNotFoundException("AttachmentStyle not found: $id") }
            },
            emotionalExpression = request.emotionalExpressionId?.let { id ->
                emotionalExpressionRepository.findById(id)
                    .orElseThrow { ResourceNotFoundException("EmotionalExpression not found: $id") }
            },
            avoidanceStyle = request.avoidanceStyleId?.let { id ->
                avoidanceStyleRepository.findById(id)
                    .orElseThrow { ResourceNotFoundException("AvoidanceStyle not found: $id") }
            },
            speechArchetype = request.speechArchetypeId?.let { id ->
                speechArchetypeRepository.findById(id)
                    .orElseThrow { ResourceNotFoundException("SpeechArchetype not found: $id") }
            },
        )
        return result.toCheckDto(axisBalanceService.buildImbalanceNote(result.imbalances))
    }

    /**
     * 성향 단계를 정한다. 지정이 없으면 v4 운영 원칙의 기본값인 3단계로 채운다.
     *
     * 빈 값을 그대로 두면 프롬프트에 "개방성: 단계 — " 같은 줄이 들어가므로, 실행 기록에도 실제 사용한 단계를 남긴다.
     */
    private fun resolveTraitStage(stageId: Long?, trait: BigFiveTrait): PersonalityTraitStageEntity =
        findTraitStage(stageId, trait)
            ?: personalityTraitStageRepository.findByTraitAndStage(trait, DEFAULT_TRAIT_STAGE)
            ?: throw ResourceNotFoundException("PersonalityTraitStage not found: $trait $DEFAULT_TRAIT_STAGE")

    /**
     * 양극단 단계는 최대 두 축까지만 허용한다. (v4 §3.3 운영 원칙)
     *
     * 세 축 이상을 극단으로 두면 축마다 파생된 행동이 서로 밀어내서 인물이 흐려진다.
     */
    private fun validateExtremeTraitStages(stages: List<PersonalityTraitStageEntity>) {
        val extremes = stages.filter { it.stage == MIN_TRAIT_STAGE || it.stage == MAX_TRAIT_STAGE }
        if (extremes.size > MAX_EXTREME_TRAIT_COUNT) {
            throw InvalidRequestException(
                "1단계 또는 5단계는 최대 ${MAX_EXTREME_TRAIT_COUNT}개 축까지만 지정할 수 있습니다: " +
                    extremes.joinToString(", ") { "${it.trait.label} ${it.stage}단계" },
            )
        }
    }

    /**
     * 상처 서사가 겹치지 않게 프롬프트에 알려줄 기존 캐릭터 목록. (v4 원칙 5)
     *
     * 지정이 없으면 최근 생성된 캐릭터를 붙이고, 빈 목록을 명시하면 아무것도 붙이지 않는다.
     */
    private fun existingCharactersNote(characterIds: List<Long>?): String? {
        val characters = when {
            characterIds == null -> characterRepository.findRecent(
                PageRequest.of(0, DEFAULT_EXISTING_CHARACTER_COUNT),
            )
            characterIds.isEmpty() -> return null
            else -> characterIds.map { id ->
                characterRepository.findOneWithGenerationById(id)
                    ?: throw ResourceNotFoundException("Character not found: $id")
            }
        }
        if (characters.isEmpty()) return null
        return characters.joinToString("\n") { existingCharacterLine(it) }
    }

    /** v4 §7 플레이스홀더 명세의 순서: 이름 / 분야 / Lie 유형 / wound 한 줄 / 나이 */
    private fun existingCharacterLine(character: Character): String = listOf(
        character.name,
        character.generationExecute.domain?.name ?: "분야 미지정",
        character.lieType,
        character.lieWound,
        "${character.age}세",
    ).joinToString(" / ")

    /**
     * 충돌은 통과시키지 않고, 불균형은 한 번에 하나까지만 통과시킨다.
     */
    private fun validateAxisBalance(result: AxisBalanceResult) {
        result.conflict?.let {
            throw InvalidRequestException(
                "${it.describe()} 조합은 같은 상황에서 서로 반대를 지시하므로 함께 지정할 수 없다. ${it.reason}",
            )
        }
        if (result.imbalances.size > AxisBalanceResult.MAX_ALLOWED_IMBALANCES) {
            throw InvalidRequestException(
                "불균형 조합은 한 번에 하나만 허용한다. 현재 ${result.imbalances.size}개다: " +
                    result.imbalances.joinToString(", ") { it.describe() },
            )
        }
    }

    private fun findTraitStage(id: Long?, expectedTrait: BigFiveTrait): PersonalityTraitStageEntity? {
        if (id == null) return null
        val stage = personalityTraitStageRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("PersonalityTraitStage not found: $id") }
        if (stage.trait != expectedTrait) {
            throw InvalidRequestException(
                "PersonalityTraitStage $id belongs to ${stage.trait}, but $expectedTrait is required",
            )
        }
        return stage
    }

    /**
     * 실행할 캐릭터 생성 프롬프트 ref를 결정한다.
     *
     * refId를 생략하면 CHARACTER_GENERATION 기본 프롬프트를 쓰고,
     * 지정하면 용도가 캐릭터 생성인 템플릿의 ref인지 검증한다.
     */
    private fun resolveGenerationRef(refId: Long?): PromptTemplateRef {
        if (refId == null) {
            return defaultPromptTemplateService.resolveDefaultRef(PromptTemplateType.CHARACTER_GENERATION)
        }
        val ref = promptTemplateRefRepository.findOneWithDetailsById(refId)
            ?: throw ResourceNotFoundException("PromptTemplateRef not found: $refId")
        if (ref.promptTemplate.type != PromptTemplateType.CHARACTER_GENERATION) {
            throw InvalidRequestException(
                "PromptTemplate type mismatch. expected=${PromptTemplateType.CHARACTER_GENERATION}, " +
                    "actual=${ref.promptTemplate.type}, refId=$refId",
            )
        }
        return ref
    }

    private companion object {
        /** v4 §3.3 운영 원칙: UI 기본값은 전부 3단계다. */
        const val DEFAULT_TRAIT_STAGE = 3
        const val MIN_TRAIT_STAGE = 1
        const val MAX_TRAIT_STAGE = 5

        /** v4 §3.3 운영 원칙: 1·5단계는 최대 2개 축까지다. */
        const val MAX_EXTREME_TRAIT_COUNT = 2

        /** 기존 캐릭터를 지정하지 않았을 때 프롬프트에 붙일 최근 캐릭터 수. */
        const val DEFAULT_EXISTING_CHARACTER_COUNT = 10
    }
}
