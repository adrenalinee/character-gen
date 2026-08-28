package ltmp.charactergen.service.prompt

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PromptPlaceholdersTests {
    @Test
    fun `치환값이 없는 placeholder는 빈 문자열이 된다`() {
        val rendered = renderPlaceholders("{{NAME}}은 {{AGE}}세다.", mapOf("NAME" to "박가람"))

        assertEquals("박가람은 세다.", rendered)
    }

    @Test
    fun `블록은 치환값이 있으면 제목과 가드까지 남는다`() {
        val template = """
            앞줄
            {{#RELATIONS}}
            ## 아는 사람
            {{RELATIONS}}
            이름과 관계 정도만 말한다.
            {{/RELATIONS}}
            뒷줄
        """.trimIndent()

        val rendered = renderPlaceholders(template, mapOf("RELATIONS" to "- 박가람 — 동료"))

        assertEquals(
            """
            앞줄
            ## 아는 사람
            - 박가람 — 동료
            이름과 관계 정도만 말한다.
            뒷줄
            """.trimIndent(),
            rendered,
        )
    }

    @Test
    fun `치환값이 비면 블록의 제목과 가드까지 사라진다`() {
        val template = """
            앞줄
            {{#RELATIONS}}
            ## 아는 사람
            {{RELATIONS}}
            이름과 관계 정도만 말한다.
            {{/RELATIONS}}
            뒷줄
        """.trimIndent()

        // 값이 없을 때와 공백만 있을 때 모두 같아야 한다.
        val missing = renderPlaceholders(template, emptyMap())
        val blank = renderPlaceholders(template, mapOf("RELATIONS" to "  \n "))

        assertEquals("앞줄\n\n뒷줄", missing)
        assertEquals(missing, blank)
    }

    @Test
    fun `서로 다른 블록이 여러 개 있어도 각자 판정된다`() {
        val template = "{{#A}}A={{A}}{{/A}}|{{#B}}B={{B}}{{/B}}|{{#C}}C={{C}}{{/C}}"

        val rendered = renderPlaceholders(template, mapOf("A" to "1", "C" to "3"))

        assertEquals("A=1||C=3", rendered)
    }

    @Test
    fun `블록 밖의 같은 키는 그대로 치환된다`() {
        val template = "{{WANT}} / {{#WANT}}{{WANT}}{{/WANT}}"

        assertEquals("가게를 지키는 것 / 가게를 지키는 것", renderPlaceholders(template, mapOf("WANT" to "가게를 지키는 것")))
        assertEquals(" / ", renderPlaceholders(template, emptyMap()))
    }

    @Test
    fun `치환값에 들어 있는 달러 기호와 역슬래시는 그대로 남는다`() {
        val rendered = renderPlaceholders("{{TEXT}}", mapOf("TEXT" to """$1 \n"""))

        assertEquals("""$1 \n""", rendered)
    }
}
