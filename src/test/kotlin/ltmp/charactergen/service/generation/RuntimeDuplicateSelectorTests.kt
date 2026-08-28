package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.model.L0CharacterResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 런타임에 넣을 [대화] 필드 선택 규칙. (L0 캐릭터 생성 프롬프트 v4.2 B-13)
 *
 * 벡터를 직접 쥐고 우선순위·임계값·상한·하한이 각각 어디서 걸리는지를 확인한다.
 * 벡터를 지정하지 않은 문장은 영벡터가 되어 무엇과도 겹치지 않는다.
 */
class RuntimeDuplicateSelectorTests {
    @Test
    fun `상위 필드와 유사도가 임계값을 넘는 신변 디테일은 제외한다`() {
        val selector = selector(
            "묘사1" to axis(0),
            "묘사2" to axis(1),
            "묘사3" to axis(2),
            "평소 행동 규칙" to axis(1),
        )

        val inclusion = selector.select(character(concreteDetails = listOf("묘사1", "묘사2", "묘사3")))

        assertEquals(setOf(0, 2), inclusion.concreteDetailIndexes)
    }

    @Test
    fun `겹치지 않는 신변 디테일이 많아도 상한까지만 남긴다`() {
        val selector = selector()

        val inclusion = selector.select(
            character(concreteDetails = listOf("묘사1", "묘사2", "묘사3", "묘사4", "묘사5")),
        )

        assertEquals(
            setOf(0, 1, 2),
            inclusion.concreteDetailIndexes,
            "상한을 넘으면 배열 앞쪽 세 개만 남아야 한다",
        )
    }

    @Test
    fun `신변 디테일이 전부 겹친다고 판정돼도 하한만큼은 남기고 경고한다`() {
        // 세 묘사 모두 평소 행동 규칙과 겹치지만 하한 2개 때문에 하나만 빠진다. (스펙 4-1)
        val selector = selector(
            "묘사1" to axis(0),
            "묘사2" to axis(0),
            "묘사3" to axis(0),
            "평소 행동 규칙" to axis(0),
        )

        val inclusion = selector.select(character(concreteDetails = listOf("묘사1", "묘사2", "묘사3")))

        assertEquals(
            setOf(1, 2),
            inclusion.concreteDetailIndexes,
            "런타임에 신변 디테일이 하한보다 적게 남으면 안 된다",
        )
        assertEquals(2, inclusion.warnings.count { it.contains("신변 디테일") })
        assertTrue(inclusion.warnings.any { it.contains("묘사2") })
    }

    @Test
    fun `상위 필드와 겹치는 말버릇은 제외하고 문장 패턴은 남긴다`() {
        // 문장 패턴은 스펙 4-1에서 비교 기준으로 옮겨져 겹침 판정으로는 빠지지 않는다.
        val selector = selector(
            "말버릇2" to axis(0),
            "정서 표출 서술" to axis(0),
            "문장 패턴" to axis(1),
            "거절당했을 때" to axis(1),
        )

        val inclusion = selector.select(character())

        assertEquals(setOf(0, 2, 3), inclusion.verbalTicIndexes)
        assertTrue(inclusion.sentencePatternIncluded)
    }

    @Test
    fun `상위 필드와 겹치는 surface는 제외하고 개방 항목은 남긴다`() {
        // 스펙 4-1에서 제거 대상과 비교 기준이 뒤집혔다. surface가 후보, 비surface 항목이 기준이다.
        val selector = selector(
            "surface1" to axis(0),
            "회피 서술" to axis(0),
        )

        val inclusion = selector.select(
            character(surface = listOf("surface1", "surface2", "surface3", "surface4")),
        )

        assertFalse(0 in inclusion.disclosureItemIndexes, "겹치는 surface 항목은 빠져야 한다")
        assertTrue(setOf(1, 2, 3).all { it in inclusion.disclosureItemIndexes })
    }

    @Test
    fun `surface는 하한 세 개 아래로 내려가지 않는다`() {
        val selector = selector(
            "surface1" to axis(0),
            "surface2" to axis(0),
            "surface3" to axis(0),
            "회피 서술" to axis(0),
        )

        val inclusion = selector.select(
            character(surface = listOf("surface1", "surface2", "surface3", "surface4")),
        )

        assertEquals(
            setOf(1, 2, 3),
            inclusion.disclosureItemIndexes.filter { it < 4 }.toSet(),
            "surface가 하한 아래로 내려가면 관계가 얕은 동안 나눌 대화가 없어진다",
        )
        assertEquals(2, inclusion.warnings.count { it.contains("가벼운 개인사") })
    }

    @Test
    fun `개수 제약이 있는 배열은 겹쳐도 건드리지 않는다`() {
        // non_negotiables, forbidden_behaviors, behavioral_rule은 후보에도 기준에도 없다. (스펙 4-1)
        val selector = selector(
            "굽히지 않는 것1" to axis(0),
            "하지 않는 것1" to axis(0),
            "평소 행동 규칙" to axis(0),
        )

        val inclusion = selector.select(character())

        assertTrue(inclusion.warnings.isEmpty(), "제거 대상이 아닌 필드로 경고가 나오면 안 된다")
    }

    @Test
    fun `겹치는 것이 없으면 모두 남긴다`() {
        val inclusion = selector().select(character())

        assertEquals(setOf(0, 1, 2, 3), inclusion.verbalTicIndexes)
        assertTrue(inclusion.sentencePatternIncluded)
        assertEquals((0..5).toSet(), inclusion.disclosureItemIndexes)
        assertTrue(inclusion.warnings.isEmpty())
    }

    private fun selector(vararg vectors: Pair<String, FloatArray>): RuntimeDuplicateSelector {
        val byText = vectors.toMap()
        val client = object : TextEmbeddingClient {
            // 벡터를 지정하지 않은 문장은 영벡터라 코사인 유사도가 언제나 0이다.
            override fun embed(texts: List<String>): List<FloatArray> =
                texts.map { byText[it] ?: FloatArray(DIMENSION) }
        }
        return RuntimeDuplicateSelector(client, SIMILARITY_THRESHOLD, MAX_CONCRETE_DETAIL_COUNT)
    }

    /** 지정한 축만 1인 단위 벡터. 서로 다른 축끼리는 코사인 0이다. */
    private fun axis(index: Int): FloatArray = FloatArray(DIMENSION).also { it[index] = 1f }

    private fun character(
        concreteDetails: List<String> = listOf("묘사1", "묘사2", "묘사3"),
        surface: List<String> = listOf("surface1", "surface2", "surface3"),
    ) = l0CharacterResponse(concreteDetails = concreteDetails, surface = surface)

    private companion object {
        const val DIMENSION = 8
        const val SIMILARITY_THRESHOLD = 0.75
        const val MAX_CONCRETE_DETAIL_COUNT = 3
    }
}
