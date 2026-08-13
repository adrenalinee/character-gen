package ltmp.charactergen.service.generation

import ltmp.charactergen.api.L0CharacterGenerationRequest
import org.springframework.stereotype.Component

@Component
class L0CharacterPromptRenderer {
    fun render(
        prompt: GenerationPrompt,
        request: L0CharacterGenerationRequest,
        options: L0GenerationOptions,
    ): GenerationPrompt {
        val replacements = replacements(request, options)
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

    private fun replacements(
        request: L0CharacterGenerationRequest,
        options: L0GenerationOptions,
    ): Map<String, String> = mapOf(
        "DESCRIPTION" to request.description.orEmpty(),
        "DOMAIN" to options.domainName.orEmpty(),
        "LIE_TYPE" to options.lieType.orEmpty(),
        "LIE_STATEMENT" to options.lieStatement.orEmpty(),
        "O_STAGE" to options.opennessStage?.toString().orEmpty(),
        "O_DESC" to options.opennessDescription.orEmpty(),
        "C_STAGE" to options.conscientiousnessStage?.toString().orEmpty(),
        "C_DESC" to options.conscientiousnessDescription.orEmpty(),
        "E_STAGE" to options.extraversionStage?.toString().orEmpty(),
        "E_DESC" to options.extraversionDescription.orEmpty(),
        "A_STAGE" to options.agreeablenessStage?.toString().orEmpty(),
        "A_DESC" to options.agreeablenessDescription.orEmpty(),
        "N_STAGE" to options.neuroticismStage?.toString().orEmpty(),
        "N_DESC" to options.neuroticismDescription.orEmpty(),
        "ATTACHMENT" to options.attachmentType.orEmpty(),
        "ATTACHMENT_DESC" to options.attachmentDescription.orEmpty(),
        "EMOTIONAL_EXPRESSION" to options.emotionalExpressionType.orEmpty(),
        "EMOTIONAL_DESC" to options.emotionalExpressionDescription.orEmpty(),
        "AVOIDANCE_STYLE" to options.avoidanceStyleType.orEmpty(),
        "AVOIDANCE_DESC" to options.avoidanceStyleDescription.orEmpty(),
        "SPEECH_ARCHETYPE" to options.speechArchetypeName.orEmpty(),
        "SPEECH_DESC" to options.speechArchetypeDescription.orEmpty(),
        "AGE" to request.age?.toString().orEmpty(),
        "GENDER" to request.gender?.name.orEmpty(),
        "SPEECH_REGISTER" to request.speechRegister.orEmpty(),
        "SCALE" to request.scale?.toString().orEmpty(),
        "EMOTION" to request.emotion?.toString().orEmpty(),
        "EXTRA_CONSTRAINTS" to request.extraConstraints.orEmpty(),
    )

    private fun replacePlaceholders(template: String, replacements: Map<String, String>): String =
        PLACEHOLDER_REGEX.replace(template) { match -> replacements[match.groupValues[1]].orEmpty() }

    private companion object {
        val PLACEHOLDER_REGEX = Regex("""\{\{([A-Z0-9_]+)}}""")
    }
}
