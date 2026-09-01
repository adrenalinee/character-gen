package ltmp.charactergen.service.generation

import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.model.RelationshipStageNote
import ltmp.charactergen.service.text.NOUN_OVERLAP_THRESHOLD
import ltmp.charactergen.service.text.sharedKoreanNouns
import org.springframework.stereotype.Component

/**
 * 생성 결과의 서술 방식을 본다. (L0 캐릭터 생성 프롬프트 v4.3 P-10)
 *
 * 두 가지를 검사한다.
 *
 * - 관계 단계의 감정·회피가 행동이 아니라 정도로 쓰였는가. v4.3의 변경 계기 자체다.
 *   "질투와 의존까지 드러낼 수 있다"는 감정의 목록이지 행동이 아니라서 런타임 모델이 실행할 수 없다.
 * - 개방 사다리의 `deep` 항목이 `lie.wound`와 같은 사건을 반복하는가. wound는 별도로 주입되므로
 *   같은 이야기가 close 단계 프롬프트에 두 번 들어간다.
 *
 * 문서는 두 검사를 하드 실패(재생성)로 두라고 적었지만, 값을 고치거나 생성을 실패시키지 않고
 * [L0CharacterCountValidator]와 같은 방식으로 경고만 만든다. 이유는 두 가지다.
 * 정도 표현 패턴은 문서 스스로 오탐이 있을 수 있다고 적어 둔 단순 문자열 매칭이라 정상 결과를 버릴 수 있고,
 * 지금 생성 경로에는 재생성 루프가 없어 하드 실패가 곧 생성 실패다.
 * 경고는 실행 기록에 남으므로 반복 생성에서 같은 경고가 쌓이면 프롬프트나 패턴 목록을 고칠 근거가 된다.
 */
@Component
class L0CharacterWordingValidator {
    fun validate(character: L0CharacterResponse): List<String> =
        degreeWordingWarnings(character) + woundDeepOverlapWarnings(character)

    /**
     * 관계 단계의 감정·회피에 정도 표현이 남아 있는지 본다.
     *
     * `attachment`와 `address`는 검사하지 않는다. 문서가 지정한 대상이 두 필드이고,
     * 애착 서술에는 "덜 "이나 "더 많이"가 정상적인 행동 서술의 일부로 들어가는 일이 잦다.
     */
    private fun degreeWordingWarnings(character: L0CharacterResponse): List<String> =
        with(character.relationshipStages) {
            listOf("early" to early, "deepening" to deepening, "close" to close)
        }.flatMap { (stageName, note) -> stageWarnings(stageName, note) }

    private fun stageWarnings(stageName: String, note: RelationshipStageNote): List<String> =
        listOf("emotional_range" to note.emotionalRange, "avoidance" to note.avoidance)
            .flatMap { (fieldName, text) ->
                DEGREE_PATTERNS.filter { text.contains(it) }.map { pattern ->
                    "관계 단계 $stageName.$fieldName 이(가) 행동이 아니라 정도로 쓰였을 수 있습니다" +
                        "(걸린 표현 '$pattern'): $text"
                }
            }

    /**
     * `deep` 항목이 `wound`와 같은 사건을 말하는지 본다. 겹치면 고쳐야 하는 쪽은 `deep`이다.
     * `wound`는 관계가 깊어졌을 때 따로 주입되는 자리가 이미 있다.
     */
    private fun woundDeepOverlapWarnings(character: L0CharacterResponse): List<String> {
        val wound = character.core.lie.wound
        return character.disclosureLadder.deep.mapNotNull { item ->
            val shared = sharedKoreanNouns(item, wound)
            if (shared.size < NOUN_OVERLAP_THRESHOLD) return@mapNotNull null
            "개방 사다리 deep 항목이 lie.wound와 같은 사건을 반복하는 것으로 보입니다" +
                "(겹치는 말 ${shared.sorted().joinToString(", ")}): $item"
        }
    }

    private companion object {
        /**
         * 정도 표현 패턴. (v4.3 P-10)
         *
         * '덜 '의 뒤 공백은 '덜다', '덜어'처럼 정도가 아닌 낱말을 걸러내기 위한 것이라 지우면 안 된다.
         */
        val DEGREE_PATTERNS = listOf(
            "까지 드러", "까지 보여", "어디까지", "얼마나", "정도만", "최소한만",
            "더 많이", "덜 ", "범위만", "수준으로",
        )
    }
}
