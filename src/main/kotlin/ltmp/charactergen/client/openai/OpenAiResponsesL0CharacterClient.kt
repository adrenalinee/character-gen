package ltmp.charactergen.client.openai

import com.openai.client.OpenAIClient
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.StructuredResponseCreateParams
import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.service.generation.GenerationPrompt
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class OpenAiResponsesL0CharacterClient(
    @Value("\${openai.model}") private val model: String,
    private val openAIClient: OpenAIClient,
) : L0CharacterLlmClient {
//    private val openAiClient: OpenAIClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
//        OpenAIOkHttpClient.fromEnv()
//    }

    override fun generate(prompt: GenerationPrompt): L0CharacterLlmResult {
        val response = openAIClient.responses().create(createParams(prompt))

        for (outputItem in response.output()) {
            val message = outputItem.message().orElse(null) ?: continue
            for (content in message.content()) {
                val character = content.outputText().orElse(null) ?: continue
                val rawJson = content.rawContent.outputText().orElseThrow().text()
                return L0CharacterLlmResult(character = character, rawJson = rawJson)
            }
        }
        throw IllegalStateException("OpenAI response did not contain structured character output")
    }

    internal fun createParams(prompt: GenerationPrompt): StructuredResponseCreateParams<L0CharacterResponse> {
        val inputItems = prompt.inputs.map { input ->
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(EasyInputMessage.Role.of(input.role.lowercase(Locale.ROOT)))
                    .content(input.inputText)
                    .build(),
            )
        }
        return ResponseCreateParams.builder()
            .instructions(prompt.instructions)
            .input(ResponseCreateParams.Input.ofResponse(inputItems))
            .text(L0CharacterResponse::class.java)
            .model(model)
            .build()
    }
}
