package ltmp.charactergen.service.prompt

private val PLACEHOLDER_REGEX = Regex("""\{\{([A-Z0-9_]+)}}""")

/**
 * `{{#KEY}} ... {{/KEY}}` 한 쌍. 같은 키의 닫는 태그까지만 잡도록 비탐욕으로 둔다.
 * 여는 태그 뒤와 닫는 태그 앞의 개행은 태그를 쓰기 위한 것이므로 블록 내용에서 제외한다.
 */
private val BLOCK_REGEX = Regex(
    """\{\{#([A-Z0-9_]+)}}\r?\n?(.*?)\r?\n?\{\{/\1}}""",
    RegexOption.DOT_MATCHES_ALL,
)

/**
 * 템플릿의 `{{PLACEHOLDER}}`를 치환값으로 바꾼다. 치환값이 없는 placeholder는 빈 문자열이 된다.
 *
 * `{{#KEY}} ... {{/KEY}}` 블록은 같은 키의 치환값이 비어 있으면 안쪽 내용까지 통째로 사라진다.
 * 섹션 제목과 가드 문장을 템플릿에 두면서도 "내용이 없으면 제목도 넣지 않는다"를 지키기 위한 것이다.
 * (런타임 프롬프트 조립 스펙 v1 1-5) 제목을 조립기에서 문자열로 붙이면 템플릿만 봐서는 전체 모습을
 * 알 수 없고, 제목과 가드 문구를 브랜치별로 바꿔가며 비교할 수도 없다.
 *
 * 블록을 먼저 처리하고 그 다음에 단일 치환을 한다. 중첩 블록은 지원하지 않는다.
 * 블록이 빠진 자리에는 빈 줄이 남으므로 호출자가 연속 개행을 정리해야 한다.
 *
 * 캐릭터 생성 프롬프트와 런타임 인스트럭션이 같은 치환 규칙을 쓰도록 한 곳에 둔다.
 */
fun renderPlaceholders(template: String, replacements: Map<String, String>): String {
    val withBlocks = BLOCK_REGEX.replace(template) { match ->
        val key = match.groupValues[1]
        if (replacements[key].isNullOrBlank()) "" else match.groupValues[2]
    }
    return PLACEHOLDER_REGEX.replace(withBlocks) { match -> replacements[match.groupValues[1]].orEmpty() }
}
