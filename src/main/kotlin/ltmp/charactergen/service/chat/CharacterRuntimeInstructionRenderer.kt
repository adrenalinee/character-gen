package ltmp.charactergen.service.chat

import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterRelationshipStageEntity
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
 * L0 캐릭터를 런타임 대화용 인스트럭션 한 벌로 조립한다. (L0 런타임 주입 설계 1~3절, 생성 프롬프트 v4 §8.1)
 *
 * v4의 소비처 구분에 따라 아래 필드는 의도적으로 넣지 않는다.
 * `oneLine`, `lieStatement`, `need`, `wantNeedConflict`, `contradictionSharedRoot`, `identityNote`,
 * 모든 `tieToLie`, `speechArchetypeNote`, `recognizability`, `longTermViability`, `imbalanceNote`.
 * 특히 `lieStatement`와 `need`는 캐릭터 자신이 모르는 것이라, 주입하면 캐릭터가 자기 믿음과 결핍을 설명하게 된다.
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
            "AGE" to character.age.toString(),
            "OCCUPATION" to character.occupation,
            "DAILY_LIFE" to character.dailyLife,
            "CONCRETE_DETAILS" to bulleted(
                character.concreteDetails.take(MAX_CONCRETE_DETAILS).map { it.detailText },
            ),
            "BEHAVIORAL_RULES" to bulleted(character.behavioralRules.map { it.ruleText }),
            "EXPRESSION_NOTE" to character.expressionNote,
            "SENTENCE_PATTERN" to character.sentencePattern,
            "VERBAL_TICS" to character.verbalTics.joinToString("\n") { "${it.situation} → ${it.description}" },
            "UNDER_STRESS" to character.underStress,
            "BANNED_WORDS" to character.bannedWords.joinToString(", ") { it.wordText },
            "EMOTIONAL_EXPRESSION_NOTE" to character.emotionalExpressionNote,
            "AVOIDANCE_NOTE" to character.avoidanceNote,
            "ON_REJECTION" to character.onRejection,
            "ON_CONFLICT" to character.onConflict,
            "ON_ABSENCE" to character.onAbsence,
            "ON_OVER_APPROACH" to character.onOverApproach,
            "STAGE_LABEL" to stage.label,
            "STAGE_ATTACHMENT" to stageNote.attachment,
            "STAGE_ADDRESS" to stageNote.address,
            "STAGE_EMOTIONAL_RANGE" to stageNote.emotionalRange,
            "STAGE_AVOIDANCE" to stageNote.avoidance,
            "CONTRADICTION_TRAIT_A" to character.contradictionTraitA,
            "CONTRADICTION_TRAIT_B" to character.contradictionTraitB,
            "NON_NEGOTIABLES" to bulleted(character.nonNegotiables.map { it.statement }),
            "FORBIDDEN_BEHAVIORS" to bulleted(character.forbiddenBehaviors.map { it.behavior }),
            "WANT" to character.want,
            "RELATIONS" to bulleted(character.relations.map { "${it.name} — ${it.relation}" }),
            "OPEN_DISCLOSURE" to openDisclosure(character, openedLevel),
            "CLOSED_TOPICS_SECTION" to closedTopicsSection(character, openedLevel, options.closedTopicMode),
            "SITUATION_SECTION" to situationSection(options.situationNote),
            "WOUND_SECTION" to woundSection(character, openedLevel),
            "VOICE_SAMPLES_SECTION" to voiceSamplesSection(character, options.includeVoiceSamples),
        )
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

    private fun openDisclosure(character: Character, openedLevel: DisclosureLevel): String {
        val openedLevels = openedLevel.openedThrough()
        val opened = character.disclosureItems.filter { it.level in openedLevels }
        if (opened.isEmpty()) return "이 사람에게 아직 꺼낼 수 있는 개인사가 없다."
        return bulleted(opened.map { it.itemText })
    }

    /**
     * 아직 열리지 않은 층위를 다루는 방식. (설계 2절 방식 A/B)
     *
     * [ClosedTopicMode.TOPIC_ONLY]에서는 항목 본문 대신 층위 이름만 노출해서,
     * 캐릭터가 "그런 주제가 있다"는 것만 알고 회피할 수 있게 한다.
     */
    private fun closedTopicsSection(
        character: Character,
        openedLevel: DisclosureLevel,
        mode: ClosedTopicMode,
    ): String {
        if (mode == ClosedTopicMode.OMIT) return ""
        val closedLevels = openedLevel.closedAbove()
        val topics = closedLevels.filter { level -> character.disclosureItems.any { it.level == level } }
        if (topics.isEmpty()) return ""
        return buildString {
            appendLine("## 말하지 않는 것")
            appendLine(bulleted(topics.map { closedTopicLabel(it) }))
            appendLine("이 주제가 나오면 위의 회피 방식대로 넘긴다. 내용은 말하지 않는다.")
        }.trimEnd()
    }

    private fun closedTopicLabel(level: DisclosureLevel): String = when (level) {
        DisclosureLevel.SURFACE -> "가벼운 신상"
        DisclosureLevel.MID -> "지금 겪고 있는 어려움"
        DisclosureLevel.DEEP -> "쉽게 말하지 않는 과거"
        DisclosureLevel.DEEPEST -> "누구에게도 말한 적 없는 것"
    }

    private fun situationSection(situationNote: String?): String {
        if (situationNote.isNullOrBlank()) return ""
        return "## 지금 네 상황\n$situationNote"
    }

    /**
     * Lie의 기원이 된 상처는 심층 층위가 열렸을 때만 넣는다. (설계 1절, v4의 조건부 대화 필드)
     */
    private fun woundSection(character: Character, openedLevel: DisclosureLevel): String {
        if (!openedLevel.opensWound()) return ""
        return "## 잘 꺼내지 않는 과거\n" +
            "${character.lieWound}\n" +
            "이 일을 먼저 꺼내지 않는다. 깊이 물어올 때만 조금씩 드러난다."
    }

    /**
     * 예시 발화는 톤 참조로만 쓴다. 그대로 재사용하는 경향이 있어 명시적으로 막는다. (설계 1절)
     */
    private fun voiceSamplesSection(character: Character, include: Boolean): String {
        if (!include || character.voiceSamples.isEmpty()) return ""
        return buildString {
            appendLine("## 톤 참조")
            appendLine(bulleted(character.voiceSamples.map { it.line }))
            appendLine("위 문장을 그대로 쓰지 마라. 길이와 온도만 참고한다.")
        }.trimEnd()
    }

    private fun bulleted(values: List<String>): String = values.joinToString("\n") { "- $it" }

    /**
     * 비어 있는 구간이 남긴 연속 개행을 정리한다.
     * 구간 placeholder가 통째로 비면 빈 줄이 겹치기 때문에 마지막에 한 번 정규화한다.
     */
    private fun tidy(instruction: String): String =
        instruction.replace(BLANK_LINES_REGEX, "\n\n").trim()

    private companion object {
        const val MAX_CONCRETE_DETAILS = 3
        val BLANK_LINES_REGEX = Regex("\n{3,}")
    }
}
