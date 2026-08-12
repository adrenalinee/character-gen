package ltmp.charactergen.service

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.api.L0GenerationExecuteDto
import ltmp.charactergen.api.toDto
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class L0CharacterGenerationService(
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val promptTemplateRefService: PromptTemplateRefService,
    private val objectMapper: ObjectMapper,
) {
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
        return generationExecuteRepository.saveAndFlush(execute).toDto()
    }

    private data class PromptRefSnapshot(
        val instructions: String,
        val inputs: List<PromptRefInputSnapshot>,
    )

    private data class PromptRefInputSnapshot(
        val role: String,
        val inputText: String,
    )
}
