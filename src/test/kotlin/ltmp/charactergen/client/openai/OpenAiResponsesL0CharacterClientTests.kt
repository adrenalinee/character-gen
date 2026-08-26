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
        assertTrue(paramsDescription.contains("relationship_stages"), paramsDescription)
        assertTrue(paramsDescription.contains("relationship_events"), paramsDescription)
        assertTrue(paramsDescription.contains("behavioral_rule"), paramsDescription)
    }

    /**
     * 여러 번 참조되는 타입은 `${'$'}defs`로 빠지고 그 자리에는 `${'$'}ref`만 남는다.
     * strict 스키마는 `${'$'}ref` 노드에 description 같은 다른 키워드가 함께 있으면 요청 자체를 거부하므로,
     * 그런 타입의 필드에는 설명을 붙이면 안 된다. 이 규칙은 스키마를 만들어봐야만 드러나서 여기서 막는다.
     */
    @Test
    fun `공유 타입 참조 자리에는 다른 키워드를 함께 두지 않는다`() {
        val params = OpenAiResponsesL0CharacterClient(
            model = "gpt-5.5",
            openAIClient = mock(),
        ).createParams(
            GenerationPrompt(
                instructions = "system instructions",
                inputs = listOf(GenerationPromptInput(role = "user", inputText = "user prompt")),
            ),
        )

        val violations = REF_WITH_SIBLING_REGEX
            .findAll(params.rawParams.toString())
            .map { it.value }
            .toList()

        assertTrue(violations.isEmpty(), "\$ref 자리에 다른 키워드가 함께 있다: $violations")
    }

    private companion object {
        /** 스키마는 map의 toString으로 직렬화되므로 `{${'$'}ref=..., 다른 키=...}` 형태를 찾는다. */
        val REF_WITH_SIBLING_REGEX = Regex("""\{\${'$'}ref=[^}]*,[^}]*}""")
    }
}
