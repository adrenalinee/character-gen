package ltmp.charactergen.service

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.api.L0GenerationExecuteDto
import ltmp.charactergen.api.L0GenerationExecuteSimpleDto
import ltmp.charactergen.api.toDto
import ltmp.charactergen.api.toSimpleDto
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
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
        generationExecuteRepository.findById(generationId)
            .orElseThrow { ResourceNotFoundException("L0GenerationExecute not found: $generationId") }
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
        val execute = L0GenerationExecute(
            promptTemplateRef = ref,
            promptRefSnapshotJson = snapshotJson,
            userInputJson = objectMapper.writeValueAsString(request),
            status = L0GenerationExecuteStatus.PREPARING,
        )
        val savedExecute = generationExecuteRepository.saveAndFlush(execute)
        taskDispatcher.dispatchAfterCommit(savedExecute.id)
        return savedExecute.toDto()
    }
}
