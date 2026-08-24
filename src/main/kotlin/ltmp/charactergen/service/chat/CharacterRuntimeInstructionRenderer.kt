package ltmp.charactergen.service.chat

import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.service.prompt.renderPlaceholders
import org.springframework.stereotype.Component

/**
 * 세션 옵션 중 인스트럭션 조립에 영향을 주는 값들.
 */
data class RuntimeInstructionOptions(
    val relationshipStage: RelationshipStage,
    val situationNote: String?,
    val includeVoiceSamples: Boolean,
    val closedTopicMode: ClosedTopicMode,
)

/**
 * L0 캐릭터를 런타임 대화용 인스트럭션 한 벌로 조립한다. (L0 런타임 주입 설계 1~3절)
 *
 * 설계 1절의 라우팅에 따라 아래 필드는 의도적으로 넣지 않는다.
 * `oneLine`, `need`, `wantNeedConflict`, `traits`(= `expressionNote`/`identityNote`),
 * 모든 `tieToLie`, `speechArchetypeNote`, `recognizability`, `longTermViability`, `imbalanceNote`.
 *
 * 호출자는 캐릭터의 지연 로딩 컬렉션이 살아 있는 트랜잭션 안에서 호출해야 한다.
 */
@Component
class CharacterRuntimeInstructionRenderer {
    fun render(template: String, character: Character, options: RuntimeInstructionOptions): String =
        tidy(renderPlaceholders(template, replacements(character, options)))

    private fun replacements(character: Character, options: RuntimeInstructionOptions): Map<String, String> {
        val stage = options.relationshipStage
        return mapOf(
            "NAME" to character.name,
            "AGE" to character.age.toString(),
            "OCCUPATION" to character.occupation,
            "DAILY_LIFE" to character.dailyLife,
            "CONCRETE_DETAILS" to bulleted(
                character.concreteDetails.take(MAX_CONCRETE_DETAILS).map { it.detailText },
            ),
            "SENTENCE_PATTERN" to character.sentencePattern,
            "VERBAL_TICS" to character.verbalTics.joinToString("\n") { "${it.situation} → ${it.description}" },
            "UNDER_STRESS" to character.underStress,
            "ADDRESS_STYLE" to character.addressStyle,
            "BANNED_WORDS" to character.bannedWords.joinToString(", ") { it.wordText },
            "EMOTIONAL_EXPRESSION_NOTE" to character.emotionalExpressionNote,
            "AVOIDANCE_NOTE" to character.avoidanceNote,
            "ATTACHMENT_NOTE" to attachmentNote(character, stage),
            "CONTRADICTION_TRAIT_A" to character.contradictionTraitA,
            "CONTRADICTION_TRAIT_B" to character.contradictionTraitB,
            "CONTRADICTION_SHARED_ROOT" to character.contradictionSharedRoot,
            "NON_NEGOTIABLES" to bulleted(character.nonNegotiables.map { it.statement }),
            "FORBIDDEN_BEHAVIORS" to bulleted(character.forbiddenBehaviors.map { it.behavior }),
            "WANT" to character.want,
            "RELATIONS" to bulleted(character.relations.map { "${it.name} — ${it.relation}" }),
            "LIE_STATEMENT" to character.lieStatement,
            "OPEN_DISCLOSURE" to openDisclosure(character, stage),
            "CLOSED_TOPICS_SECTION" to closedTopicsSection(character, stage, options.closedTopicMode),
            "SITUATION_SECTION" to situationSection(options.situationNote),
            "WOUND_SECTION" to woundSection(character, stage),
            "VOICE_SAMPLES_SECTION" to voiceSamplesSection(character, options.includeVoiceSamples),
        )
    }

    /**
     * 현재 단계의 애착 서술 하나를 넣는다.
     * 거절 반응은 어느 단계에서든 일어나므로 단계와 무관하게 항상 넣는다. (설계 3절)
     */
    private fun attachmentNote(character: Character, stage: RelationshipStage): String {
        val staged = when (stage) {
            RelationshipStage.FIRST -> character.attachmentNoteEarly
            RelationshipStage.FAMILIAR -> character.attachmentNoteDeepening
            RelationshipStage.CLOSE, RelationshipStage.SPECIAL -> character.attachmentNoteClose
        }
        return "$staged\n거절당하면: ${character.attachmentNoteOnRejection}"
    }

    private fun openDisclosure(character: Character, stage: RelationshipStage): String {
        val opened = character.disclosureItems.filter { it.level in stage.openedLevels }
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
        stage: RelationshipStage,
        mode: ClosedTopicMode,
    ): String {
        if (mode == ClosedTopicMode.OMIT) return ""
        val closedLevels = stage.closedLevels()
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
     * Lie의 기원이 된 상처는 심층 층위가 열린 단계에서만 넣는다. (설계 1절)
     */
    private fun woundSection(character: Character, stage: RelationshipStage): String {
        if (!stage.opensWound()) return ""
        return "이 믿음은 다음에서 왔다: ${character.lieWound}\n" +
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
