package ltmp.charactergen.service.generation

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class L0CharacterPromptRenderer(
    private val lieRepository: LieRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
) {
    @Transactional(readOnly = true)
    fun render(prompt: GenerationPrompt, request: L0CharacterGenerationRequest): GenerationPrompt {
        val replacements = replacements(request)
        return GenerationPrompt(
            instructions = replacePlaceholders(prompt.instructions, replacements),
            inputs = prompt.inputs.map {
                GenerationPromptInput(
                    role = it.role,
                    inputText = replacePlaceholders(it.inputText, replacements),
                )
            },
        )
    }

    private fun replacements(request: L0CharacterGenerationRequest): Map<String, String> = mapOf(
        "DESCRIPTION" to request.description.orEmpty(),
        "DOMAIN" to request.domain,
        "LIE_TYPE" to request.lieType,
        "LIE_STATEMENT" to lieRepository.findByType(request.lieType)?.coreStatement.orEmpty(),
        "O_STAGE" to request.opennessStage.toString(),
        "O_DESC" to traitDescription(BigFiveTrait.OPENNESS, request.opennessStage),
        "C_STAGE" to request.conscientiousnessStage.toString(),
        "C_DESC" to traitDescription(BigFiveTrait.CONSCIENTIOUSNESS, request.conscientiousnessStage),
        "E_STAGE" to request.extraversionStage.toString(),
        "E_DESC" to traitDescription(BigFiveTrait.EXTRAVERSION, request.extraversionStage),
        "A_STAGE" to request.agreeablenessStage.toString(),
        "A_DESC" to traitDescription(BigFiveTrait.AGREEABLENESS, request.agreeablenessStage),
        "N_STAGE" to request.neuroticismStage.toString(),
        "N_DESC" to traitDescription(BigFiveTrait.NEUROTICISM, request.neuroticismStage),
        "ATTACHMENT" to request.attachment,
        "ATTACHMENT_DESC" to attachmentStyleRepository.findByType(request.attachment)?.description.orEmpty(),
        "EMOTIONAL_EXPRESSION" to request.emotionalExpression,
        "EMOTIONAL_DESC" to emotionalExpressionRepository.findByType(request.emotionalExpression)?.description.orEmpty(),
        "AVOIDANCE_STYLE" to request.avoidanceStyle,
        "AVOIDANCE_DESC" to avoidanceStyleRepository.findByType(request.avoidanceStyle)?.description.orEmpty(),
        "SPEECH_ARCHETYPE" to request.speechArchetype,
        "SPEECH_DESC" to speechArchetypeRepository.findByName(request.speechArchetype)?.description.orEmpty(),
        "AGE" to request.age.toString(),
        "GENDER" to request.gender,
        "SPEECH_REGISTER" to request.speechRegister,
        "SCALE" to request.scale.toString(),
        "EMOTION" to request.emotion.toString(),
        "EXTRA_CONSTRAINTS" to request.extraConstraints.orEmpty(),
    )

    private fun traitDescription(trait: BigFiveTrait, stage: Int): String =
        personalityTraitStageRepository.findByTraitAndStage(trait, stage)?.description.orEmpty()

    private fun replacePlaceholders(template: String, replacements: Map<String, String>): String =
        PLACEHOLDER_REGEX.replace(template) { match -> replacements[match.groupValues[1]].orEmpty() }

    private companion object {
        val PLACEHOLDER_REGEX = Regex("""\{\{([A-Z0-9_]+)}}""")
    }
}
