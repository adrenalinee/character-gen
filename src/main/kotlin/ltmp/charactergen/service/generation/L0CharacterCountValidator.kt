package ltmp.charactergen.service.generation

import ltmp.charactergen.model.L0CharacterResponse
import org.springframework.stereotype.Component

/**
 * 개수 자체가 캐릭터 정의의 일부인 배열이 규정 개수를 채웠는지 본다. (런타임 프롬프트 조립 스펙 v1 4-1)
 *
 * 스펙 6절 11번은 `forbidden_behaviors`가 3개 미만으로 줄어드는 원인을 중복 제거로 보고 보호 대상 지정을
 * 요구하지만, [RuntimeDuplicateSelector]는 이 배열들을 후보에도 기준에도 넣은 적이 없어 애초에 건드리지
 * 않는다. 실제 원인은 LLM에 나가는 구조화 출력 스키마에 개수 제약이 하나도 없었다는 것이고,
 * 그쪽은 `L0CharacterResponse`의 `@ArraySchema`로 막았다.
 *
 * 이 검증은 그 뒤에 남는 백스톱이다. 값을 채우거나 자르지 않고 경고만 만든다.
 * 개수가 모자란 캐릭터도 대화는 가능하고, 모자란 자리를 서버가 임의의 값으로 메우면
 * 그 값의 출처를 아무도 설명할 수 없게 되기 때문이다.
 */
@Component
class L0CharacterCountValidator {
    fun validate(character: L0CharacterResponse): List<String> = buildList {
        checkCount("반드시 지키는 것(non_negotiables)", character.core.nonNegotiables.size, 3, 3)
        checkCount("절대 하지 않는 것(forbidden_behaviors)", character.core.forbiddenBehaviors.size, 3, 5)
        checkCount("평소 행동(lie.behavioral_rule)", character.core.lie.behavioralRule.size, 2, 3)
        checkCount("말버릇(voice.verbal_tics)", character.voice.verbalTics.size, 4, 4)
        checkCount("가벼운 개인사(disclosure_ladder.surface)", character.disclosureLadder.surface.size, 3, 5)
        checkCount("신변 디테일(identity.concrete_details)", character.identity.concreteDetails.size, 3, 5)
    }

    private fun MutableList<String>.checkCount(label: String, actual: Int, min: Int, max: Int) {
        if (actual in min..max) return
        val expected = if (min == max) "$min" else "$min~$max"
        add("$label 개수가 규정과 다릅니다: 규정 ${expected}개, 실제 ${actual}개")
    }
}
