package ltmp.charactergen.service.text

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoreanNounsTests {
    @Test
    fun `조사를 벗기고 같은 낱말로 모은다`() {
        assertEquals(setOf("연재", "편집자", "원고"), koreanNouns("연재에서 편집자가 원고를"))
    }

    @Test
    fun `조사를 벗기면 한 글자만 남는 말은 그대로 둔다`() {
        // '뒤로'의 '로'를 벗기면 '뒤'만 남는다. 벗긴 결과가 낱말 구실을 못 하면 조사가 아니다.
        assertTrue(koreanNouns("그 뒤로").contains("뒤로"))
    }

    @Test
    fun `한 글자와 불용어는 버린다`() {
        val nouns = koreanNouns("그 사람 때문에 다시 원고를 고쳤다")

        assertFalse(nouns.contains("그"), "한 글자는 조사를 벗긴 찌꺼기와 구별되지 않는다")
        assertFalse(nouns.contains("사람"))
        assertFalse(nouns.contains("다시"))
        assertTrue(nouns.contains("원고"))
    }

    @Test
    fun `숫자와 문장부호는 낱말 경계로만 쓴다`() {
        assertEquals(setOf("연재", "마감"), koreanNouns("연재 3화, 마감!"))
    }

    @Test
    fun `같은 사건을 말하는 두 문장은 임계값 이상 겹친다`() {
        val shared = sharedKoreanNouns(
            "첫 연재에서 편집자가 원고를 반려하고 다른 작가로 교체했다",
            "그 연재에서 편집자가 원고를 반려한 뒤로 마감을 앞당겼다",
        )

        assertTrue(shared.size >= NOUN_OVERLAP_THRESHOLD, "겹치는 말: $shared")
    }

    @Test
    fun `다른 사건을 말하는 두 문장은 임계값 미만으로 겹친다`() {
        val shared = sharedKoreanNouns("첫 연재에서 교체됐다", "새벽에 산책하고 국수를 먹는다")

        assertTrue(shared.size < NOUN_OVERLAP_THRESHOLD, "겹치는 말: $shared")
    }
}
