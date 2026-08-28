package ltmp.charactergen.client.openai

import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.service.generation.GenerationPrompt
import ltmp.charactergen.service.generation.GenerationPromptInput
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

        // v4.1에서 traits 블록이 사라지고 그 검토용 내용이 design_note.trait_reflection으로 옮겨졌다. (B-1)
        assertTrue(paramsDescription.contains("trait_reflection"), paramsDescription)
        assertFalse(paramsDescription.contains("traits="), paramsDescription)
        assertFalse(paramsDescription.contains("identity_note"), paramsDescription)

        // v4.2에서 under_stress가 emotional_expression_note로 흡수됐다. (B-2, B-3)
        assertFalse(paramsDescription.contains("under_stress"), paramsDescription)
        assertTrue(
            paramsDescription.contains("감정이 격해졌을 때 말과 행동이 어떻게 변하는지를 함께 쓴다"),
            paramsDescription,
        )
    }

    /**
     * 개수 제약은 프롬프트 본문이 아니라 스키마로 나가야 지켜진다. (런타임 프롬프트 조립 스펙 v1 6절 11번)
     *
     * 스펙은 `forbidden_behaviors`가 3개 미만으로 줄어드는 원인을 중복 제거로 보지만,
     * [ltmp.charactergen.service.generation.RuntimeDuplicateSelector]는 이 필드를 후보에도 기준에도
     * 넣지 않아 애초에 건드리지 않는다. 실제 원인은 이 클래스에서 만들어지는 스키마에 개수 제약이
     * 하나도 없었다는 것이다. 참고용 `l0_character_schema_v4.2.json`의 minItems는 전송되지 않는다.
     *
     * SDK는 victools에 Swagger2Module을 등록하므로 `@ArraySchema`가 스키마로 옮겨지고,
     * SDK의 로컬 검증기도 배열 하위 스키마에서 minItems/maxItems를 허용한다.
     * 둘 중 하나라도 깨지면 이 테스트가 먼저 실패한다.
     */
    @Test
    fun `배열 필드의 개수 제약이 구조화 출력 스키마에 실린다`() {
        val params = OpenAiResponsesL0CharacterClient(
            model = "gpt-5.5",
            openAIClient = mock(),
        ).createParams(
            GenerationPrompt(
                instructions = "system instructions",
                inputs = listOf(GenerationPromptInput(role = "user", inputText = "user prompt")),
            ),
        )

        val paramsDescription = params.rawParams.toString()
        assertTrue(paramsDescription.contains("minItems"), paramsDescription)
        assertTrue(paramsDescription.contains("maxItems"), paramsDescription)
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
