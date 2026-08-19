package ltmp.charactergen.service.generation

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.data.BigFiveVector
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

        val request = L0CharacterGenerationRequest(
            refId = execute.promptTemplateRef.id,
            description = execute.description,
            name = execute.name,
            domainId = execute.domain?.id,
            lieId = execute.lie?.id,
            woundTimingId = execute.woundTiming?.id,
            woundPlaceId = execute.woundPlace?.id,
            woundAgentId = execute.woundAgent?.id,
            woundFormId = execute.woundForm?.id,
            woundDurationId = execute.woundDuration?.id,
            opennessStageId = execute.opennessStage?.id,
            conscientiousnessStageId = execute.conscientiousnessStage?.id,
            extraversionStageId = execute.extraversionStage?.id,
            agreeablenessStageId = execute.agreeablenessStage?.id,
            neuroticismStageId = execute.neuroticismStage?.id,
            attachmentStyleId = execute.attachmentStyle?.id,
            emotionalExpressionId = execute.emotionalExpression?.id,
            avoidanceStyleId = execute.avoidanceStyle?.id,
            speechArchetypeId = execute.speechArchetype?.id,
            age = execute.age,
            gender = execute.gender,
            speechRegisterId = execute.speechRegister?.id,
            scale = execute.scale,
            emotion = execute.emotion,
            extraConstraints = execute.extraConstraints,
        )
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
        return L0GenerationWork(
            request = request,
            prompt = prompt,
            options = L0GenerationOptions(
                domainName = execute.domain?.name,
                lieType = execute.lie?.type,
                lieStatement = execute.lie?.coreStatement,
                woundTimingValue = execute.woundTiming?.value,
                woundPlaceValue = execute.woundPlace?.value,
                woundAgentValue = execute.woundAgent?.value,
                woundFormValue = execute.woundForm?.value,
                woundDurationValue = execute.woundDuration?.value,
                opennessStage = execute.opennessStage?.stage,
                opennessDescription = execute.opennessStage?.description,
                conscientiousnessStage = execute.conscientiousnessStage?.stage,
                conscientiousnessDescription = execute.conscientiousnessStage?.description,
                extraversionStage = execute.extraversionStage?.stage,
                extraversionDescription = execute.extraversionStage?.description,
                agreeablenessStage = execute.agreeablenessStage?.stage,
                agreeablenessDescription = execute.agreeablenessStage?.description,
                neuroticismStage = execute.neuroticismStage?.stage,
                neuroticismDescription = execute.neuroticismStage?.description,
                attachmentType = execute.attachmentStyle?.type,
                attachmentDescription = execute.attachmentStyle?.description,
                attachmentVector = execute.attachmentStyle?.bigFiveVector,
                emotionalExpressionType = execute.emotionalExpression?.type,
                emotionalExpressionDescription = execute.emotionalExpression?.description,
                emotionalExpressionVector = execute.emotionalExpression?.bigFiveVector,
                avoidanceStyleType = execute.avoidanceStyle?.type,
                avoidanceStyleDescription = execute.avoidanceStyle?.description,
                avoidanceStyleVector = execute.avoidanceStyle?.bigFiveVector,
                speechArchetypeName = execute.speechArchetype?.name,
                speechArchetypeDescription = execute.speechArchetype?.description,
                speechArchetypeVector = execute.speechArchetype?.bigFiveVector,
                speechRegisterName = execute.speechRegister?.name,
            ),
        )
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

        val character = characterRepository.save(response.toEntity(execute))
        execute.character = character
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
    val options: L0GenerationOptions,
)

data class L0GenerationOptions(
    val domainName: String? = null,
    val lieType: String? = null,
    val lieStatement: String? = null,
    val woundTimingValue: String? = null,
    val woundPlaceValue: String? = null,
    val woundAgentValue: String? = null,
    val woundFormValue: String? = null,
    val woundDurationValue: String? = null,
    val opennessStage: Int? = null,
    val opennessDescription: String? = null,
    val conscientiousnessStage: Int? = null,
    val conscientiousnessDescription: String? = null,
    val extraversionStage: Int? = null,
    val extraversionDescription: String? = null,
    val agreeablenessStage: Int? = null,
    val agreeablenessDescription: String? = null,
    val neuroticismStage: Int? = null,
    val neuroticismDescription: String? = null,
    val attachmentType: String? = null,
    val attachmentDescription: String? = null,
    val attachmentVector: BigFiveVector? = null,
    val emotionalExpressionType: String? = null,
    val emotionalExpressionDescription: String? = null,
    val emotionalExpressionVector: BigFiveVector? = null,
    val avoidanceStyleType: String? = null,
    val avoidanceStyleDescription: String? = null,
    val avoidanceStyleVector: BigFiveVector? = null,
    val speechArchetypeName: String? = null,
    val speechArchetypeDescription: String? = null,
    val speechArchetypeVector: BigFiveVector? = null,
    val speechRegisterName: String? = null,
)
