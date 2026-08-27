package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.model.CharacterBehavior
import ltmp.charactergen.model.CharacterContradiction
import ltmp.charactergen.model.CharacterCore
import ltmp.charactergen.model.CharacterIdentity
import ltmp.charactergen.model.CharacterLie
import ltmp.charactergen.model.DisclosureLadder
import ltmp.charactergen.model.L0CharacterResponse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

/**
 * 런타임에 넣을 `concrete_details` 선택 규칙. (L0 캐릭터 생성 프롬프트 v4.1 B-10)
 *
 * 벡터를 직접 쥐고 임계값·상한·하한이 각각 어디서 걸리는지를 확인한다.
 */
class RuntimeConcreteDetailSelectorTests {
    @Test
    fun `비교 대상과 유사도가 임계값을 넘는 항목은 제외한다`() {
        // 묘사2만 비교 대상과 같은 축을 가리킨다.
        val selector = selector(
            "묘사1" to axis(0),
            "묘사2" to axis(1),
            "묘사3" to axis(2),
            SURFACE to axis(1),
            EXPRESSION to axis(3),
            RULE to axis(4),
        )

        assertEquals(setOf(0, 2), selector.select(character(listOf("묘사1", "묘사2", "묘사3"))))
    }

    @Test
    fun `겹치지 않는 항목이 많아도 상한까지만 남긴다`() {
        val selector = selector(
            "묘사1" to axis(0),
            "묘사2" to axis(1),
            "묘사3" to axis(2),
            "묘사4" to axis(3),
            "묘사5" to axis(4),
            SURFACE to axis(5),
            EXPRESSION to axis(6),
            RULE to axis(7),
        )

        assertEquals(
            setOf(0, 1, 2),
            selector.select(character(listOf("묘사1", "묘사2", "묘사3", "묘사4", "묘사5"))),
            "상한을 넘으면 배열 앞쪽 세 개만 남아야 한다",
        )
    }

    @Test
    fun `전부 겹친다고 판정돼도 가장 덜 겹치는 두 개는 남긴다`() {
        // 세 묘사 모두 비교 대상과 임계값 이상으로 겹치되, 겹치는 정도는 서로 다르게 둔다.
        val selector = selector(
            "묘사1" to vector(0.90f, 0.44f, 0f),
            "묘사2" to vector(1f, 0f, 0f),
            "묘사3" to vector(0.95f, 0.31f, 0f),
            SURFACE to axis(0),
            EXPRESSION to axis(2),
            RULE to axis(2),
        )

        assertEquals(
            setOf(0, 2),
            selector.select(character(listOf("묘사1", "묘사2", "묘사3"))),
            "런타임에 신변 디테일이 하나도 남지 않으면 안 된다",
        )
    }

    @Test
    fun `하한 이하로는 판정하지 않고 그대로 둔다`() {
        val selector = selector()

        assertEquals(
            setOf(0, 1),
            selector.select(character(listOf("묘사1", "묘사2"))),
            "두 개 이하는 어떻게 판정해도 전부 남으므로 임베딩을 부르지 않아야 한다",
        )
    }

    private fun selector(vararg vectors: Pair<String, FloatArray>): RuntimeConcreteDetailSelector {
        val byText = vectors.toMap()
        val client = object : TextEmbeddingClient {
            override fun embed(texts: List<String>): List<FloatArray> = texts.map { text ->
                byText[text] ?: error("테스트 벡터가 지정되지 않은 문장입니다: $text")
            }
        }
        return RuntimeConcreteDetailSelector(client, SIMILARITY_THRESHOLD, MAX_COUNT)
    }

    private fun character(concreteDetails: List<String>): L0CharacterResponse {
        val response = mock(L0CharacterResponse::class.java)
        val identity = mock(CharacterIdentity::class.java)
        val core = mock(CharacterCore::class.java)
        val lie = mock(CharacterLie::class.java)
        val behavior = mock(CharacterBehavior::class.java)
        val ladder = mock(DisclosureLadder::class.java)
        org.mockito.Mockito.`when`(identity.concreteDetails).thenReturn(concreteDetails)
        org.mockito.Mockito.`when`(lie.behavioralRule).thenReturn(listOf(RULE))
        org.mockito.Mockito.`when`(core.lie).thenReturn(lie)
        org.mockito.Mockito.`when`(behavior.emotionalExpressionNote).thenReturn(EXPRESSION)
        org.mockito.Mockito.`when`(ladder.surface).thenReturn(listOf(SURFACE))
        org.mockito.Mockito.`when`(response.identity).thenReturn(identity)
        org.mockito.Mockito.`when`(response.core).thenReturn(core)
        org.mockito.Mockito.`when`(response.behavior).thenReturn(behavior)
        org.mockito.Mockito.`when`(response.disclosureLadder).thenReturn(ladder)
        return response
    }

    /** 지정한 축만 1인 단위 벡터. 서로 다른 축끼리는 코사인 0이다. */
    private fun axis(index: Int): FloatArray = FloatArray(DIMENSION).also { it[index] = 1f }

    private fun vector(vararg values: Float): FloatArray =
        FloatArray(DIMENSION).also { values.forEachIndexed { index, value -> it[index] = value } }

    private companion object {
        const val DIMENSION = 8
        const val SIMILARITY_THRESHOLD = 0.75
        const val MAX_COUNT = 3
        const val SURFACE = "surface 항목"
        const val EXPRESSION = "정서 표출 서술"
        const val RULE = "평소 행동 규칙"
    }
}
