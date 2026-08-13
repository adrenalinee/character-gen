package ltmp.charactergen.service

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.api.L0GenerationExecuteDto
import ltmp.charactergen.api.L0GenerationExecuteSimpleDto
import ltmp.charactergen.api.toDto
import ltmp.charactergen.api.toSimpleDto
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.service.generation.PromptRefInputSnapshot
import ltmp.charactergen.service.generation.PromptRefSnapshot
import ltmp.charactergen.service.generation.L0GenerationTaskDispatcher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class L0CharacterGenerationService(
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
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
        val ref = promptTemplateRefRepository.findOneWithDetailsById(request.refId)
            ?: throw ResourceNotFoundException("PromptTemplateRef not found: ${request.refId}")
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
        val opennessStage = findTraitStage(request.opennessStageId, BigFiveTrait.OPENNESS)
        val conscientiousnessStage = findTraitStage(
            request.conscientiousnessStageId,
            BigFiveTrait.CONSCIENTIOUSNESS,
        )
        val extraversionStage = findTraitStage(request.extraversionStageId, BigFiveTrait.EXTRAVERSION)
        val agreeablenessStage = findTraitStage(request.agreeablenessStageId, BigFiveTrait.AGREEABLENESS)
        val neuroticismStage = findTraitStage(request.neuroticismStageId, BigFiveTrait.NEUROTICISM)
        val attachmentStyle = request.attachmentStyleId?.let { id ->
            attachmentStyleRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("AttachmentStyle not found: $id") }
        }
        val emotionalExpression = request.emotionalExpressionId?.let { id ->
            emotionalExpressionRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("EmotionalExpression not found: $id") }
        }
        val avoidanceStyle = request.avoidanceStyleId?.let { id ->
            avoidanceStyleRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("AvoidanceStyle not found: $id") }
        }
        val speechArchetype = request.speechArchetypeId?.let { id ->
            speechArchetypeRepository.findById(id)
                .orElseThrow { ResourceNotFoundException("SpeechArchetype not found: $id") }
        }
        val execute = L0GenerationExecute(
            promptTemplateRef = ref,
            domain = domain,
            lie = lie,
            opennessStage = opennessStage,
            conscientiousnessStage = conscientiousnessStage,
            extraversionStage = extraversionStage,
            agreeablenessStage = agreeablenessStage,
            neuroticismStage = neuroticismStage,
            attachmentStyle = attachmentStyle,
            emotionalExpression = emotionalExpression,
            avoidanceStyle = avoidanceStyle,
            speechArchetype = speechArchetype,
            description = request.description,
            age = request.age,
            gender = request.gender,
            speechRegister = request.speechRegister,
            scale = request.scale,
            emotion = request.emotion,
            extraConstraints = request.extraConstraints,
            promptRefSnapshotJson = snapshotJson,
            userInputJson = objectMapper.writeValueAsString(request),
            status = L0GenerationExecuteStatus.PREPARING,
        )
        val savedExecute = generationExecuteRepository.saveAndFlush(execute)
        taskDispatcher.dispatchAfterCommit(savedExecute.id)
        return savedExecute.toDto()
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
}
