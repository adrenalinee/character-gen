package ltmp.charactergen.service.chat

import ltmp.charactergen.api.character.CharacterRuntimeInstructionDto
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.service.DefaultPromptTemplateService
import ltmp.charactergen.service.InvalidRequestException
import ltmp.charactergen.service.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 캐릭터의 저장된 설정과 런타임 프롬프트 템플릿을 조합한다.
 *
 * 대화 세션 생성과 인스트럭션 조회가 같은 조립 경로를 사용하도록 한 곳에 둔다.
 */
@Service
class CharacterRuntimeInstructionService(
    private val characterRepository: CharacterRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val defaultPromptTemplateService: DefaultPromptTemplateService,
    private val instructionRenderer: CharacterRuntimeInstructionRenderer,
) {
    @Transactional(readOnly = true)
    fun generateDefault(
        characterId: Long,
        relationshipStage: RelationshipStage,
    ): CharacterRuntimeInstructionDto = assemble(
        RuntimeInstructionAssemblyRequest(
            characterId = characterId,
            relationshipStage = relationshipStage,
        ),
    ).toDto()

    /**
     * 인스트럭션 조립 결과를 반환한다. 대화 세션 서비스는 이 결과를 저장해 이후의 스트림 호출에서 재사용한다.
     * 호출자는 캐릭터의 지연 로딩 컬렉션을 읽을 수 있는 트랜잭션 안에서 호출해야 한다.
     */
    fun assemble(request: RuntimeInstructionAssemblyRequest): AssembledRuntimeInstruction {
        val character = characterRepository.findOneWithGenerationById(request.characterId)
            ?: throw ResourceNotFoundException("Character not found: ${request.characterId}")
        val ref = resolveRuntimeRef(request.refId, request.instructionBranch)
        val options = RuntimeInstructionOptions(
            relationshipStage = request.relationshipStage,
            // 개방 층위를 주지 않으면 단계에서 파생한다. deepest는 명시해야만 열린다.
            openedDisclosureLevel = request.openedDisclosureLevel
                ?: request.relationshipStage.defaultOpenedLevel,
            situationNote = request.situationNote?.takeIf { it.isNotBlank() },
            // 예시 발화는 관계가 얕은 사용자를 전제로 생성된다. 그래서 기본은 early에서만 넣는다. (v4.2 B-11)
            includeVoiceSamples = request.includeVoiceSamples
                ?: (request.relationshipStage == RelationshipStage.EARLY),
            closedTopicMode = request.closedTopicMode ?: ClosedTopicMode.TOPIC_ONLY,
        )
        return AssembledRuntimeInstruction(
            characterId = character.id,
            characterName = character.name,
            refId = ref.id,
            instructionBranch = ref.name,
            options = options,
            instructions = instructionRenderer.render(ref.instructions, character, options),
        )
    }

    /**
     * 대화에 사용할 런타임 인스트럭션 ref를 결정한다.
     *
     * refId를 지정하면 용도가 CHARACTER_RUNTIME인 템플릿의 ref인지 검증하고,
     * instructionBranch를 지정하면 기본 CHARACTER_RUNTIME 템플릿 안에서 그 브랜치를 찾는다.
     * 둘 다 없으면 기본 프롬프트를 쓴다.
     */
    private fun resolveRuntimeRef(refId: Long?, instructionBranch: String?): PromptTemplateRef {
        if (refId != null && instructionBranch != null) {
            throw InvalidRequestException("refId와 instructionBranch는 동시에 지정할 수 없습니다.")
        }
        if (refId != null) {
            val ref = promptTemplateRefRepository.findOneWithDetailsById(refId)
                ?: throw ResourceNotFoundException("PromptTemplateRef not found: $refId")
            if (ref.promptTemplate.type != PromptTemplateType.CHARACTER_RUNTIME) {
                throw InvalidRequestException(
                    "PromptTemplate type mismatch. expected=${PromptTemplateType.CHARACTER_RUNTIME}, " +
                        "actual=${ref.promptTemplate.type}, refId=$refId",
                )
            }
            return ref
        }
        if (instructionBranch != null) {
            val template = defaultPromptTemplateService.resolveDefaultTemplate(PromptTemplateType.CHARACTER_RUNTIME)
            return promptTemplateRefRepository
                .findOneWithDetailsByPromptTemplateIdAndName(template.id, instructionBranch)
                ?: throw ResourceNotFoundException(
                    "PromptTemplateRef branch not found: templateId=${template.id}, branchName=$instructionBranch",
                )
        }
        return defaultPromptTemplateService.resolveDefaultRef(PromptTemplateType.CHARACTER_RUNTIME)
    }
}

data class RuntimeInstructionAssemblyRequest(
    val characterId: Long,
    val relationshipStage: RelationshipStage,
    val openedDisclosureLevel: DisclosureLevel? = null,
    val situationNote: String? = null,
    val refId: Long? = null,
    val instructionBranch: String? = null,
    val includeVoiceSamples: Boolean? = null,
    val closedTopicMode: ClosedTopicMode? = null,
)

data class AssembledRuntimeInstruction(
    val characterId: Long,
    val characterName: String,
    val refId: Long,
    val instructionBranch: String?,
    val options: RuntimeInstructionOptions,
    val instructions: String,
) {
    fun toDto(): CharacterRuntimeInstructionDto = CharacterRuntimeInstructionDto(
        characterId = characterId,
        characterName = characterName,
        relationshipStage = options.relationshipStage,
        openedDisclosureLevel = options.openedDisclosureLevel,
        refId = refId,
        instructionBranch = instructionBranch,
        instructions = instructions,
        includeVoiceSamples = options.includeVoiceSamples,
        closedTopicMode = options.closedTopicMode,
    )
}
