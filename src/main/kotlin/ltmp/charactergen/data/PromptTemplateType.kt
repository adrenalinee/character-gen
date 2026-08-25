package ltmp.charactergen.data

/**
 * 프롬프트 템플릿의 용도다.
 *
 * 용도가 다른 프롬프트를 서로 바꿔 실행하면 완전히 엉뚱한 결과가 나오므로,
 * 실행 시점에 요청받은 ref의 템플릿 타입이 그 용도와 일치하는지 검증한다.
 */
enum class PromptTemplateType(val label: String) {
    CHARACTER_GENERATION("캐릭터 생성"),
    CHARACTER_RUNTIME("캐릭터 런타임 인스트럭션"),
}
