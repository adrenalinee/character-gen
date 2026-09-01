package ltmp.charactergen.service.generation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class L0CharacterWordingValidatorTests {
    private val validator = L0CharacterWordingValidator()

    @Test
    fun `행동으로 쓴 관계 단계와 겹치지 않는 개방 항목은 경고를 만들지 않는다`() {
        assertEquals(emptyList(), validator.validate(l0CharacterResponse()))
    }

    @Test
    fun `관계 단계의 감정과 회피가 정도로 쓰이면 경고한다`() {
        val warnings = validator.validate(
            l0CharacterResponse(
                stageEmotionalRange = "질투와 의존까지 드러낼 수 있다",
                stageAvoidance = "핵심 사실은 최소한만 남겨 둔다",
            ),
        )

        // 세 단계 각각의 두 필드가 모두 걸린다.
        assertEquals(6, warnings.size)
        assertTrue(warnings.any { it.contains("early.emotional_range") && it.contains("까지 드러") })
        assertTrue(warnings.any { it.contains("close.avoidance") && it.contains("최소한만") })
    }

    @Test
    fun `행동으로 쓴 감정과 회피는 정도 표현으로 보지 않는다`() {
        val warnings = validator.validate(
            l0CharacterResponse(
                stageEmotionalRange = "상대가 다른 사람 이야기를 오래 하면 화제를 자기 작업으로 돌린다",
                stageAvoidance = "가족 이야기가 나오면 상대의 질문을 되물어 화제를 넘긴다",
            ),
        )

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `deep 항목이 상처와 같은 사건을 반복하면 경고한다`() {
        val warnings = validator.validate(
            l0CharacterResponse(
                wound = "첫 연재에서 편집자가 원고를 반려하고 다른 작가로 교체했다",
                deep = listOf(
                    "그 연재에서 편집자가 원고를 반려한 뒤로 마감을 앞당겼다",
                    "새벽에 산책하다 국수를 먹는 버릇이 그때 생겼다",
                ),
            ),
        )

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("마감을 앞당겼다"))
    }
}
