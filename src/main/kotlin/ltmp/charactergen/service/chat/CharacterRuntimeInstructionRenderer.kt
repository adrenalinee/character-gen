package ltmp.charactergen.service.chat

import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterRelationEntity
import ltmp.charactergen.data.entity.CharacterRelationshipStageEntity
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.service.prompt.renderPlaceholders
import org.springframework.stereotype.Component

/**
 * 세션 옵션 중 인스트럭션 조립에 영향을 주는 값들.
 *
 * 관계 단계와 개방 층위는 v4에서 서로 다른 축이다. 전자는 어떤 단계 서술을 주입할지를 정하고,
 * 후자는 개인사를 어디까지 열지를 정한다. 그래서 둘을 따로 받는다.
 */
data class RuntimeInstructionOptions(
    val relationshipStage: RelationshipStage,
    val openedDisclosureLevel: DisclosureLevel,
    val situationNote: String?,
    val includeVoiceSamples: Boolean,
    val closedTopicMode: ClosedTopicMode,
)

/**
 * L0 캐릭터를 런타임 대화용 인스트럭션 한 벌로 조립한다.
 * (런타임 프롬프트 조립 스펙 v1, L0 런타임 주입 설계 1~3절, 생성 프롬프트 v4.2 C-6, C-7)
 *
 * v4.2 C-6의 소비처 구분에 따라 아래 필드는 의도적으로 넣지 않는다.
 * `oneLine`, `lieStatement`, `lieType`, `need`, `wantNeedConflict`, `contradictionSharedRoot`,
 * 모든 `tieToLie`, `speechArchetypeNote`, `recognizability`, `longTermViability`, `imbalanceNote`, `traitReflection`.
 * 특히 `lieStatement`와 `need`는 캐릭터 자신이 모르는 것이라, 주입하면 캐릭터가 자기 믿음과 결핍을 설명하게 된다.
 *
 * **섹션 제목과 가드 문장은 이 클래스가 아니라 템플릿에 있다.** 조립기는 각 구간의 본문만 만들고,
 * 본문이 비면 빈 문자열을 돌려준다. 템플릿의 `{{#KEY}} ... {{/KEY}}` 블록이 그때 제목과 가드까지 함께
 * 지운다. (스펙 1-5) 제목을 여기서 문자열로 붙이면 템플릿만 봐서는 프롬프트 전체 모습을 알 수 없고,
 * 제목·가드 문구를 브랜치별로 바꿔가며 비교할 수도 없다.
 *
 * 호출자는 캐릭터의 지연 로딩 컬렉션이 살아 있는 트랜잭션 안에서 호출해야 한다.
 */
@Component
class CharacterRuntimeInstructionRenderer {
    fun render(template: String, character: Character, options: RuntimeInstructionOptions): String =
        tidy(renderPlaceholders(template, replacements(character, options)))

    private fun replacements(character: Character, options: RuntimeInstructionOptions): Map<String, String> {
        val stage = options.relationshipStage
        val openedLevel = options.openedDisclosureLevel
        val stageNote = stageNote(character, stage)
        return mapOf(
            "NAME" to character.name,
            "IDENTITY_LINES" to identityLines(character),
            "CONCRETE_DETAILS" to bulleted(
                character.concreteDetails.filter { it.runtimeIncluded }.map { it.detailText },
            ),
            "BEHAVIORAL_RULES" to bulleted(character.behavioralRules.map { it.ruleText }),
            "SENTENCE_PATTERN_LINE" to sentencePatternLine(character),
            "VERBAL_TICS" to bulleted(
                character.verbalTics
                    .filter { it.runtimeIncluded }
                    .map { "${it.situation} → ${it.description}" },
            ),
            "BANNED_WORDS" to character.bannedWords.joinToString(", ") { it.wordText },
            "EMOTIONAL_EXPRESSION_NOTE" to character.emotionalExpressionNote,
            "AVOIDANCE_NOTE" to character.avoidanceNote,
            "ON_REJECTION" to character.onRejection,
            "ON_CONFLICT" to character.onConflict,
            "ON_ABSENCE" to character.onAbsence,
            "ON_OVER_APPROACH" to character.onOverApproach,
            "STAGE_LABEL" to stage.promptLabel,
            "STAGE_ATTACHMENT" to stageNote.attachment,
            "STAGE_ADDRESS" to stageNote.address,
            "STAGE_EMOTIONAL_RANGE" to stageNote.emotionalRange,
            "STAGE_AVOIDANCE" to stageNote.avoidance,
            "CONTRADICTION_TRAIT_A" to character.contradictionTraitA,
            "CONTRADICTION_TRAIT_B" to character.contradictionTraitB,
            "NON_NEGOTIABLES" to bulleted(character.nonNegotiables.map { it.statement }),
            "FORBIDDEN_BEHAVIORS" to bulleted(character.forbiddenBehaviors.map { it.behavior }),
            "WANT" to character.want,
            "RELATIONS" to bulleted(activeRelations(character).map { "${it.name} — ${it.relation}" }),
            "ABSENT_RELATIONS" to absentRelations(character, openedLevel),
            "SURFACE_DISCLOSURE" to surfaceDisclosure(character),
            "OPENED_DISCLOSURE" to openedDisclosure(character, stage, openedLevel),
            "CLOSED_TOPICS" to closedTopics(character, openedLevel, options.closedTopicMode),
            "SITUATION" to options.situationNote.orEmpty(),
            "WOUND" to wound(character, openedLevel),
            "VOICE_SAMPLES" to voiceSamples(character, options.includeVoiceSamples),
        )
    }

    /**
     * 나이와 직업, 그리고 일상. (스펙 3-1)
     *
     * `occupation`의 핵심 명사구가 `daily_life`에 이미 들어 있으면 `daily_life` 줄을 생략한다.
     * 같은 말이 두 줄에 걸쳐 반복되면 모델이 그 소재만 붙들고 이야기한다.
     *
     * 판정은 임베딩이 아니라 문자열 비교로 한다. 짧은 직업 명사구와 긴 일상 서술은 뜻이 겹쳐도
     * 코사인 유사도가 겹침 임계치까지 올라오지 않는 경우가 많고, 이 판정은 생성 시점이 아니라
     * 조립 시점에 필요해서 임베딩 호출을 새로 붙여야 하기 때문이다.
     */
    private fun identityLines(character: Character): String {
        val head = "${character.age}세. ${character.occupation}".let {
            if (it.last().isLetterOrDigit()) "$it." else it
        }
        if (occupationRepeatedInDailyLife(character.occupation, character.dailyLife)) return head
        return "$head\n${character.dailyLife}"
    }

    /**
     * 공백과 문장부호를 지운 뒤, 직업 전체가 일상 서술에 들어 있거나
     * 두 문자열의 최장 공통 부분문자열이 직업의 대부분을 차지하면 반복으로 본다.
     *
     * 예: "생활 다큐멘터리 사진가" / "생활 다큐멘터리 촬영과 ..." → 공통 "생활다큐멘터리"가 직업의 70%.
     */
    private fun occupationRepeatedInDailyLife(occupation: String, dailyLife: String): Boolean {
        val left = normalizeForComparison(occupation)
        val right = normalizeForComparison(dailyLife)
        if (left.isEmpty() || right.isEmpty()) return false
        if (right.contains(left)) return true
        val common = longestCommonSubstringLength(left, right)
        return common >= MIN_REPEATED_NOUN_PHRASE_LENGTH &&
            common.toDouble() / left.length >= REPEATED_NOUN_PHRASE_RATIO
    }

    private fun normalizeForComparison(value: String): String =
        value.filter { it.isLetterOrDigit() }

    private fun longestCommonSubstringLength(left: String, right: String): Int {
        var best = 0
        var previous = IntArray(right.length + 1)
        for (i in 1..left.length) {
            val current = IntArray(right.length + 1)
            for (j in 1..right.length) {
                if (left[i - 1] == right[j - 1]) {
                    current[j] = previous[j - 1] + 1
                    if (current[j] > best) best = current[j]
                }
            }
            previous = current
        }
        return best
    }

    /**
     * 현재 관계 깊이에 해당하는 단계 서술 한 벌을 고른다. (v4 §8.1의 단계별 주입)
     *
     * 관계 사건 네 가지는 어느 단계에서든 일어나므로 여기서 고르지 않고 항상 주입한다.
     * 개방 층위도 이 단계에서 파생하지 않는다. 어디까지 열지는 서버가 따로 판정해 옵션으로 넘긴다.
     */
    private fun stageNote(character: Character, stage: RelationshipStage): CharacterRelationshipStageEntity =
        character.relationshipStages.firstOrNull { it.stage == stage }
            ?: error("캐릭터에 $stage 관계 단계 서술이 없습니다: characterId=${character.id}")

    /**
     * 처음 만난 사람에게도 말하는 것. 관계 깊이와 무관하게 항상 열려 있다.
     */
    private fun surfaceDisclosure(character: Character): String {
        val surface = character.disclosureItems
            .filter { it.level == DisclosureLevel.SURFACE && it.runtimeIncluded }
        if (surface.isEmpty()) return "이 사람에게 아직 꺼낼 수 있는 개인사가 없다."
        return bulleted(surface.map { it.itemText })
    }

    /**
     * 관계가 깊어지면서 열린 개인사. (v4.2 B-12, C-8)
     *
     * v4.1에서는 이 항목들이 surface와 한 목록에 섞여 층위 구분이 사라졌다. 그래서 섹션을 따로 둔다.
     *
     * 얕은 관계에서 surface만 열어 두면 초기 대화의 소재가 금방 바닥난다. 상한 모델(C-8)은
     * early에서도 mid를 조건부로 열지만 그 조건을 판정할 로직이 아직 없다. 그래서 임시로
     * early에서만 mid 한 항목을 무조건 연다. 개방 조건 판정이 붙으면 이 규칙은 그쪽으로 교체된다.
     *
     * 개방 층위가 깊어질수록 항목이 누적되므로 공개 범위 구간만 길이 예산(스펙 4-2)을 넘길 수 있다.
     * surface가 최대 5개이므로 여기서 [MAX_OPENED_DISCLOSURE_ITEMS]개로 잘라 두 섹션 합계를 10줄로 맞춘다.
     */
    private fun openedDisclosure(
        character: Character,
        stage: RelationshipStage,
        openedLevel: DisclosureLevel,
    ): String {
        val openedLevels = openedLevel.openedThrough() - DisclosureLevel.SURFACE
        val opened = character.disclosureItems
            .filter { it.level in openedLevels && it.runtimeIncluded }
            .toMutableList()
        if (stage == RelationshipStage.EARLY && DisclosureLevel.MID !in openedLevels) {
            character.disclosureItems
                .firstOrNull { it.level == DisclosureLevel.MID && it.runtimeIncluded }
                ?.let { opened.add(it) }
        }
        return bulleted(opened.take(MAX_OPENED_DISCLOSURE_ITEMS).map { it.itemText })
    }

    /**
     * 평상시 문장 경향. 더 우선순위가 높은 필드와 겹친다고 판정되면(v4.2 B-13) 라벨째로 빠진다.
     */
    private fun sentencePatternLine(character: Character): String {
        if (!character.sentencePatternRuntimeIncluded) return ""
        return "문장: ${character.sentencePattern}"
    }

    /**
     * 지금 실제로 만나는 사람만 추린다. (v4.1 B-9)
     *
     * `presence`가 '부재'인 인물까지 한 줄에 나열하면 캐릭터가 그 사람을 요즘도 보는 것처럼 말하게 된다.
     * 활성 인물이 많아도 캐릭터가 누구 얘기를 할지 산만해지므로 상한을 둔다. 아크 플래너가 붙으면
     * 여기서 현재 아크에 관여하는 인물만 고르게 된다.
     */
    private fun activeRelations(character: Character): List<CharacterRelationEntity> =
        character.relations
            .filter { it.presence == RelationPresence.현존 }
            .take(MAX_ACTIVE_RELATIONS)

    /**
     * 지금은 만나지 않는 사람. 심층이 열린 뒤에만 꺼낼 수 있다. (v4.1 B-9)
     */
    private fun absentRelations(character: Character, openedLevel: DisclosureLevel): String {
        if (!openedLevel.opensWound()) return ""
        val absent = character.relations.filter { it.presence == RelationPresence.부재 }
        return bulleted(absent.map { "${it.name} — ${it.relation}" })
    }

    /**
     * 아직 열리지 않은 층위를 다루는 방식. (설계 2절 방식 A/B)
     *
     * [ClosedTopicMode.TOPIC_ONLY]에서는 항목 본문 대신 층위 이름만 노출해서,
     * 캐릭터가 "그런 주제가 있다"는 것만 알고 회피할 수 있게 한다.
     */
    private fun closedTopics(
        character: Character,
        openedLevel: DisclosureLevel,
        mode: ClosedTopicMode,
    ): String {
        if (mode == ClosedTopicMode.OMIT) return ""
        val closedLevels = openedLevel.closedAbove()
        val topics = closedLevels.filter { level -> character.disclosureItems.any { it.level == level } }
        return bulleted(topics.map { closedTopicLabel(it) })
    }

    private fun closedTopicLabel(level: DisclosureLevel): String = when (level) {
        DisclosureLevel.SURFACE -> "가벼운 신상"
        DisclosureLevel.MID -> "지금 겪고 있는 어려움"
        DisclosureLevel.DEEP -> "쉽게 말하지 않는 과거"
        DisclosureLevel.DEEPEST -> "누구에게도 말한 적 없는 것"
    }

    /**
     * Lie의 기원이 된 상처는 심층 층위가 열렸을 때만 넣는다. (설계 1절, v4의 조건부 대화 필드)
     */
    private fun wound(character: Character, openedLevel: DisclosureLevel): String {
        if (!openedLevel.opensWound()) return ""
        return character.lieWound
    }

    /**
     * 예시 발화는 톤 참조로만 쓴다. 그대로 재사용하는 경향이 있어 템플릿에서 명시적으로 막는다.
     * (설계 1절, v4.2 B-11)
     *
     * `situation` 라벨은 넣지 않는다. 상황과 문장이 짝지어져 있으면 같은 상황에서 그 문장을 그대로 쓴다.
     * 라벨을 떼면 톤·길이·온도만 전달된다.
     */
    private fun voiceSamples(character: Character, include: Boolean): String {
        if (!include) return ""
        return bulleted(character.voiceSamples.map { it.line })
    }

    private fun bulleted(values: List<String>): String = values.joinToString("\n") { "- $it" }

    /**
     * 비어 있는 구간이 남긴 연속 개행을 정리한다.
     * 블록이 통째로 빠지면 빈 줄이 겹치기 때문에 마지막에 한 번 정규화한다.
     */
    private fun tidy(instruction: String): String =
        instruction.replace(BLANK_LINES_REGEX, "\n\n").trim()

    private companion object {
        const val MAX_ACTIVE_RELATIONS = 5

        /** 공개 범위 구간을 길이 예산 안에 두기 위한 개방 항목 상한. (스펙 4-2) */
        const val MAX_OPENED_DISCLOSURE_ITEMS = 5

        /** 직업과 일상 서술이 겹쳤다고 볼 최소 공통 길이와 비율. */
        const val MIN_REPEATED_NOUN_PHRASE_LENGTH = 4
        const val REPEATED_NOUN_PHRASE_RATIO = 0.6

        val BLANK_LINES_REGEX = Regex("\n{3,}")
    }
}
