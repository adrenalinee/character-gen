package ltmp.charactergen.service.prompt

private val PLACEHOLDER_REGEX = Regex("""\{\{([A-Z0-9_]+)}}""")

/**
 * 템플릿의 `{{PLACEHOLDER}}`를 치환값으로 바꾼다. 치환값이 없는 placeholder는 빈 문자열이 된다.
 *
 * 캐릭터 생성 프롬프트와 런타임 인스트럭션이 같은 치환 규칙을 쓰도록 한 곳에 둔다.
 */
fun renderPlaceholders(template: String, replacements: Map<String, String>): String =
    PLACEHOLDER_REGEX.replace(template) { match -> replacements[match.groupValues[1]].orEmpty() }
