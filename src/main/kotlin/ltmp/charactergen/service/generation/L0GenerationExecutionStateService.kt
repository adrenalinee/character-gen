package ltmp.charactergen.service.generation

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.model.L0CharacterResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime

@Service
class L0GenerationExecutionStateService(
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val characterRepository: CharacterRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun markRunning(generationId: Long): Boolean {
        val execute = generationExecuteRepository.findById(generationId).orElse(null) ?: return false
        if (execute.status != L0GenerationExecuteStatus.PREPARING) return false
        execute.status = L0GenerationExecuteStatus.RUNNING
        execute.message = "OpenAI Responses API 호출 중"
        return true
    }

    @Transactional(readOnly = true)
    fun loadWork(generationId: Long): L0GenerationWork {
        val execute = generationExecuteRepository.findOneWithPromptById(generationId)
            ?: throw IllegalStateException("L0GenerationExecute not found while running: $generationId")
        check(execute.status == L0GenerationExecuteStatus.RUNNING) {
            "L0GenerationExecute is not RUNNING: $generationId (${execute.status})"
        }

        val request = objectMapper.readValue(execute.userInputJson, L0CharacterGenerationRequest::class.java)
        val prompt = if (execute.promptTemplateRef.type == PromptTemplateRefType.BRANCH) {
            val snapshotJson = requireNotNull(execute.promptRefSnapshotJson) {
                "BRANCH generation has no prompt snapshot: $generationId"
            }
            objectMapper.readValue(snapshotJson, PromptRefSnapshot::class.java).let {
                GenerationPrompt(
                    instructions = it.instructions,
                    inputs = it.inputs.map { input ->
                        GenerationPromptInput(role = input.role, inputText = input.inputText)
                    },
                )
            }
        } else {
            GenerationPrompt(
                instructions = execute.promptTemplateRef.instructions,
                inputs = execute.promptTemplateRef.inputs.map {
                    GenerationPromptInput(role = it.role, inputText = it.inputText)
                },
            )
        }
        return L0GenerationWork(request = request, prompt = prompt)
    }

    @Transactional
    fun complete(
        generationId: Long,
        response: L0CharacterResponse,
        rawJson: String,
        elapsedMillis: Long,
    ) {
        val execute = generationExecuteRepository.findById(generationId).orElseThrow {
            IllegalStateException("L0GenerationExecute not found while completing: $generationId")
        }
        check(execute.status == L0GenerationExecuteStatus.RUNNING) {
            "L0GenerationExecute is not RUNNING: $generationId (${execute.status})"
        }
        check(!characterRepository.existsByGenerationExecuteId(generationId)) {
            "Character already exists for L0GenerationExecute: $generationId"
        }

        characterRepository.save(response.toEntity(execute))
        execute.llmOutput = rawJson
        execute.status = L0GenerationExecuteStatus.COMPLETED
        execute.message = "캐릭터 생성 완료"
        execute.elapsed = elapsedMillis
        execute.completedAt = OffsetDateTime.now()
    }

    @Transactional
    fun fail(generationId: Long, failure: Exception, elapsedMillis: Long) {
        val execute = generationExecuteRepository.findById(generationId).orElse(null) ?: return
        if (execute.status == L0GenerationExecuteStatus.COMPLETED ||
            execute.status == L0GenerationExecuteStatus.CANCELLED
        ) {
            return
        }
        execute.status = L0GenerationExecuteStatus.FAILED
        execute.message = failure.message?.take(MAX_MESSAGE_LENGTH) ?: failure.javaClass.simpleName
        execute.errorTrace = failure.stackTraceToString()
        execute.elapsed = elapsedMillis
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 2_000
    }
}

data class L0GenerationWork(
    val request: L0CharacterGenerationRequest,
    val prompt: GenerationPrompt,
)
