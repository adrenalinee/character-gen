package ltmp.charactergen.client.openai

import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.service.generation.GenerationPrompt
import ltmp.charactergen.service.generation.GenerationPromptInput
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAiResponsesL0CharacterClientTests {
    @Test
    fun `L0CharacterResponse 클래스로 Responses API 구조화 출력 스키마를 만든다`() {
        val params = OpenAiResponsesL0CharacterClient(
            model = "gpt-5.5",
            openAIClient = mock(),
        ).createParams(
            GenerationPrompt(
                instructions = "system instructions",
                inputs = listOf(GenerationPromptInput(role = "user", inputText = "user prompt")),
            ),
        )

        assertEquals(L0CharacterResponse::class.java, params.responseType)
        val paramsDescription = params.rawParams.toString()
        assertTrue(paramsDescription.contains("gpt-5.5"))
        assertTrue(paramsDescription.contains("disclosure_ladder"))
        assertTrue(paramsDescription.contains("voice_samples"))
        assertTrue(paramsDescription.contains("design_note"))
        assertTrue(paramsDescription.contains("관계를 다루는 방식"), paramsDescription)
        assertTrue(paramsDescription.contains("과거"), paramsDescription)
    }
}
