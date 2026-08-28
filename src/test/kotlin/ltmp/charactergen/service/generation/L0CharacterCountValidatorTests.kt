package ltmp.charactergen.service.generation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 개수 규정 검증. (런타임 프롬프트 조립 스펙 v1 4-1, 6절 11번)
 *
 * 개수가 캐릭터 정의의 일부인 배열이 규정을 벗어났는지만 본다. 값은 고치지 않는다.
 */
class L0CharacterCountValidatorTests {
    private val validator = L0CharacterCountValidator()

    @Test
    fun `규정 개수를 모두 채우면 경고가 없다`() {
        assertTrue(validator.validate(l0CharacterResponse()).isEmpty())
    }

    @Test
    fun `절대 하지 않는 것이 세 개 미만이면 경고한다`() {
        // 스펙이 급하다고 표시한 사례다. 실제 원인은 스키마에 개수 제약이 없던 것이고 여기는 백스톱이다.
        val warnings = validator.validate(l0CharacterResponse(forbiddenBehaviorCount = 2))

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("forbidden_behaviors"), warnings.single())
        assertTrue(warnings.single().contains("규정 3~5개, 실제 2개"), warnings.single())
    }

    @Test
    fun `개수가 정확히 정해진 배열은 넘쳐도 경고한다`() {
        val warnings = validator.validate(l0CharacterResponse(nonNegotiableCount = 4))

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("규정 3개, 실제 4개"), warnings.single())
    }

    @Test
    fun `여러 배열이 동시에 모자라면 각각 경고한다`() {
        val warnings = validator.validate(
            l0CharacterResponse(
                behavioralRules = listOf("하나뿐인 규칙"),
                verbalTicCount = 3,
                surface = listOf("surface1", "surface2"),
            ),
        )

        assertEquals(3, warnings.size)
        assertTrue(warnings.any { it.contains("behavioral_rule") })
        assertTrue(warnings.any { it.contains("verbal_tics") })
        assertTrue(warnings.any { it.contains("surface") })
    }
}
