package ltmp.charactergen.service.chat

import ltmp.charactergen.config.L0_RUNTIME_INSTRUCTION_V1
import ltmp.charactergen.data.BannedWordSource
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterBannedWordEntity
import ltmp.charactergen.data.entity.CharacterConcreteDetailEntity
import ltmp.charactergen.data.entity.CharacterDisclosureItemEntity
import ltmp.charactergen.data.entity.CharacterForbiddenBehaviorEntity
import ltmp.charactergen.data.entity.CharacterNonNegotiableEntity
import ltmp.charactergen.data.entity.CharacterRelationEntity
import ltmp.charactergen.data.entity.CharacterVerbalTicEntity
import ltmp.charactergen.data.entity.CharacterVoiceSampleEntity
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.model.NonNegotiableDomain
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.model.RelationSlot
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * L0 런타임 주입 설계 1절의 필드 라우팅과 3절의 관계 단계 분기가 실제로 지켜지는지 검증한다.
 * 여기가 깨지면 이후 프로브 수치는 의미를 잃는다.
 */
class CharacterRuntimeInstructionRendererTests {
    private val renderer = CharacterRuntimeInstructionRenderer()

    @Test
    fun `설계 근거와 요약 필드는 인스트럭션에 주입되지 않는다`() {
        val instruction = render(RelationshipStage.SPECIAL)

        // 설계 1절에서 '제외'로 분류한 필드들
        EXCLUDED_MARKERS.forEach { marker ->
            assertFalse(instruction.contains(marker), "제외되어야 할 값이 주입되었다: $marker")
        }
    }

    @Test
    fun `상시 주입 필드는 모두 인스트럭션에 들어간다`() {
        val instruction = render(RelationshipStage.FIRST)

        listOf(
            "서윤",
            "웹툰 작가",
            "밤에만 작업한다",
            "짧은 결론 뒤 비유",
            "거절할 때",
            "선을 다시 긋는다고 말한다",
            "문장이 더 짧아진다",
            "기본적으로 존댓말",
            "감정을 사실로 바꾼다",
            "한 문장 뒤 침묵한다",
            "먼저 선을 긋는다",
            "떠나면 붙잡는다",
            "대체될 두려움",
            "비교하지 않는다",
            "상대를 시험하지 않는다",
            "자기 방식의 작품을 남긴다",
            "민호 — 전 편집자",
            "나는 대체된다",
        ).forEach { expected ->
            assertTrue(instruction.contains(expected), "상시 주입 값이 빠졌다: $expected")
        }
    }

    @Test
    fun `금지어는 두 출처를 병합해 한 줄로 넣는다`() {
        val instruction = render(RelationshipStage.FIRST)

        assertTrue(instruction.contains("쓰지 않는 말: 무조건, 당연히"))
    }

    @Test
    fun `구체 묘사는 세 개까지만 넣는다`() {
        val instruction = render(RelationshipStage.FIRST)

        assertTrue(instruction.contains("묘사1"))
        assertTrue(instruction.contains("묘사3"))
        assertFalse(instruction.contains("묘사4"), "구체 묘사는 3개를 넘기지 않아야 한다")
    }

    @Test
    fun `관계 단계에 해당하는 애착 서술만 넣고 거절 반응은 항상 넣는다`() {
        val first = render(RelationshipStage.FIRST)
        assertTrue(first.contains("먼저 묻지 않는다"))
        assertFalse(first.contains("작업 이야기만 연다"))
        assertFalse(first.contains("거리를 유지한 채 곁에 둔다"))

        val familiar = render(RelationshipStage.FAMILIAR)
        assertTrue(familiar.contains("작업 이야기만 연다"))
        assertFalse(familiar.contains("먼저 묻지 않는다"))

        val close = render(RelationshipStage.CLOSE)
        assertTrue(close.contains("거리를 유지한 채 곁에 둔다"))
        assertFalse(close.contains("먼저 묻지 않는다"))

        // 거절은 어느 단계에서든 일어나므로 단계와 무관하게 들어간다
        RelationshipStage.entries.forEach { stage ->
            assertTrue(
                render(stage).contains("한 번 물러난 뒤 다시 묻지 않는다"),
                "on_rejection이 빠졌다: $stage",
            )
        }
    }

    @Test
    fun `열리지 않은 층위의 개인사 본문은 주입되지 않는다`() {
        val first = render(RelationshipStage.FIRST)
        assertTrue(first.contains("마감 습관"))
        assertFalse(first.contains("편집자 이야기"))
        assertFalse(first.contains("교체 경험"))
        assertFalse(first.contains("자기 가치에 대한 공포"))

        val close = render(RelationshipStage.CLOSE)
        assertTrue(close.contains("교체 경험"))
        assertFalse(close.contains("자기 가치에 대한 공포"))

        val special = render(RelationshipStage.SPECIAL)
        assertTrue(special.contains("자기 가치에 대한 공포"))
    }

    @Test
    fun `닫힌 주제는 방식 A에서 통째로 빠지고 방식 B에서는 주제 이름만 남는다`() {
        val topicOnly = render(RelationshipStage.FIRST, closedTopicMode = ClosedTopicMode.TOPIC_ONLY)
        assertTrue(topicOnly.contains("## 말하지 않는 것"))
        assertTrue(topicOnly.contains("쉽게 말하지 않는 과거"))
        assertFalse(topicOnly.contains("교체 경험"), "주제 이름만 넣어야 하는데 본문이 샜다")

        val omit = render(RelationshipStage.FIRST, closedTopicMode = ClosedTopicMode.OMIT)
        assertFalse(omit.contains("## 말하지 않는 것"), "방식 A에서는 헤더도 남으면 안 된다")
        assertFalse(omit.contains("쉽게 말하지 않는 과거"))
    }

    @Test
    fun `모든 층위가 열린 단계에서는 닫힌 주제 구간이 사라진다`() {
        val special = render(RelationshipStage.SPECIAL, closedTopicMode = ClosedTopicMode.TOPIC_ONLY)

        assertFalse(special.contains("## 말하지 않는 것"))
    }

    @Test
    fun `예시 발화를 끄면 헤더까지 사라진다`() {
        val included = render(RelationshipStage.FIRST, includeVoiceSamples = true)
        assertTrue(included.contains("## 톤 참조"))
        assertTrue(included.contains("그 선은 제가 정할게요."))
        assertTrue(included.contains("위 문장을 그대로 쓰지 마라"))

        val excluded = render(RelationshipStage.FIRST, includeVoiceSamples = false)
        assertFalse(excluded.contains("## 톤 참조"), "구간을 비울 때 헤더가 남으면 안 된다")
        assertFalse(excluded.contains("그 선은 제가 정할게요."))
    }

    @Test
    fun `상황을 주지 않으면 상황 구간이 사라진다`() {
        val withSituation = render(RelationshipStage.FIRST, situationNote = "오늘 원고를 크게 반려당했다")
        assertTrue(withSituation.contains("## 지금 네 상황"))
        assertTrue(withSituation.contains("오늘 원고를 크게 반려당했다"))

        val withoutSituation = render(RelationshipStage.FIRST, situationNote = null)
        assertFalse(withoutSituation.contains("## 지금 네 상황"))
    }

    @Test
    fun `상처는 심층 층위가 열린 단계에서만 주입된다`() {
        assertFalse(render(RelationshipStage.FIRST).contains("첫 연재에서 교체됐다"))
        assertFalse(render(RelationshipStage.FAMILIAR).contains("첫 연재에서 교체됐다"))
        assertTrue(render(RelationshipStage.CLOSE).contains("첫 연재에서 교체됐다"))
        assertTrue(render(RelationshipStage.SPECIAL).contains("첫 연재에서 교체됐다"))
    }

    @Test
    fun `치환되지 않은 placeholder와 빈 줄이 남지 않는다`() {
        RelationshipStage.entries.forEach { stage ->
            listOf(ClosedTopicMode.OMIT, ClosedTopicMode.TOPIC_ONLY).forEach { mode ->
                listOf(true, false).forEach { includeSamples ->
                    val instruction = render(stage, mode, includeSamples, situationNote = null)
                    assertFalse(
                        instruction.contains("{{"),
                        "치환되지 않은 placeholder가 남았다: $stage/$mode/$includeSamples",
                    )
                    assertFalse(
                        instruction.contains("\n\n\n"),
                        "빈 구간이 연속 빈 줄을 남겼다: $stage/$mode/$includeSamples",
                    )
                }
            }
        }
    }

    @Test
    fun `조립된 인스트럭션은 캐릭터 선언으로 시작한다`() {
        assertEquals(
            "너는 서윤이다. 아래는 너 자신에 대한 것이며, 사용자에게 설명하지 않는다.",
            render(RelationshipStage.FIRST).lineSequence().first(),
        )
    }

    private fun render(
        stage: RelationshipStage,
        closedTopicMode: ClosedTopicMode = ClosedTopicMode.TOPIC_ONLY,
        includeVoiceSamples: Boolean = true,
        situationNote: String? = null,
    ): String = renderer.render(
        L0_RUNTIME_INSTRUCTION_V1,
        character(),
        RuntimeInstructionOptions(
            relationshipStage = stage,
            situationNote = situationNote,
            includeVoiceSamples = includeVoiceSamples,
            closedTopicMode = closedTopicMode,
        ),
    )

    private fun character(): Character = Character(
        generationExecute = mock(L0GenerationExecute::class.java),
        name = "서윤",
        age = 29,
        gender = "여성",
        occupation = "웹툰 작가",
        dailyLife = "밤에만 작업한다",
        oneLine = MARKER_ONE_LINE,
        lieType = "대체 가능성 공포형",
        lieStatement = "나는 대체된다",
        lieWound = "첫 연재에서 교체됐다",
        want = "자기 방식의 작품을 남긴다",
        need = MARKER_NEED,
        wantNeedConflict = MARKER_WANT_NEED_CONFLICT,
        contradictionTraitA = "먼저 선을 긋는다",
        contradictionTraitB = "떠나면 붙잡는다",
        contradictionSharedRoot = "대체될 두려움",
        expressionNote = MARKER_EXPRESSION_NOTE,
        identityNote = MARKER_IDENTITY_NOTE,
        attachmentNoteEarly = "먼저 묻지 않는다",
        attachmentNoteDeepening = "작업 이야기만 연다",
        attachmentNoteClose = "거리를 유지한 채 곁에 둔다",
        attachmentNoteOnRejection = "한 번 물러난 뒤 다시 묻지 않는다",
        emotionalExpressionNote = "감정을 사실로 바꾼다",
        avoidanceNote = "한 문장 뒤 침묵한다",
        speechArchetypeNote = MARKER_SPEECH_ARCHETYPE_NOTE,
        sentencePattern = "짧은 결론 뒤 비유",
        underStress = "문장이 더 짧아진다",
        addressStyle = "기본적으로 존댓말",
        recognizability = MARKER_RECOGNIZABILITY,
        longTermViability = MARKER_LONG_TERM_VIABILITY,
        imbalanceNote = MARKER_IMBALANCE_NOTE,
        concreteDetails = (1..4).map { CharacterConcreteDetailEntity(detailText = "묘사$it") },
        nonNegotiables = listOf(
            CharacterNonNegotiableEntity(
                domain = NonNegotiableDomain.`관계를 다루는 방식`,
                statement = "비교하지 않는다",
                tieToLie = MARKER_TIE_TO_LIE_NON_NEGOTIABLE,
                userFacing = true,
            ),
        ),
        forbiddenBehaviors = listOf(
            CharacterForbiddenBehaviorEntity(
                behavior = "상대를 시험하지 않는다",
                tieToLie = MARKER_TIE_TO_LIE_FORBIDDEN,
            ),
        ),
        verbalTics = listOf(
            CharacterVerbalTicEntity(situation = "거절할 때", description = "선을 다시 긋는다고 말한다"),
        ),
        bannedWords = listOf(
            CharacterBannedWordEntity(source = BannedWordSource.FROM_LIE, wordText = "무조건"),
            CharacterBannedWordEntity(source = BannedWordSource.FROM_BACKGROUND, wordText = "당연히"),
        ),
        relations = listOf(
            CharacterRelationEntity(
                slot = RelationSlot.`과거`,
                name = "민호",
                relation = "전 편집자",
                description = MARKER_RELATION_DESCRIPTION,
                presence = RelationPresence.`부재`,
                tieToLie = MARKER_TIE_TO_LIE_RELATION,
            ),
        ),
        disclosureItems = listOf(
            CharacterDisclosureItemEntity(level = DisclosureLevel.SURFACE, itemText = "마감 습관"),
            CharacterDisclosureItemEntity(level = DisclosureLevel.MID, itemText = "편집자 이야기"),
            CharacterDisclosureItemEntity(level = DisclosureLevel.DEEP, itemText = "교체 경험"),
            CharacterDisclosureItemEntity(level = DisclosureLevel.DEEPEST, itemText = "자기 가치에 대한 공포"),
        ),
        voiceSamples = listOf(
            CharacterVoiceSampleEntity(
                situation = "의견에 반대했을 때",
                line = "그 선은 제가 정할게요.",
                demonstrates = MARKER_VOICE_SAMPLE_DEMONSTRATES,
            ),
        ),
    )

    private companion object {
        const val MARKER_ONE_LINE = "MARKER_ONE_LINE"
        const val MARKER_NEED = "MARKER_NEED"
        const val MARKER_WANT_NEED_CONFLICT = "MARKER_WANT_NEED_CONFLICT"
        const val MARKER_EXPRESSION_NOTE = "MARKER_EXPRESSION_NOTE"
        const val MARKER_IDENTITY_NOTE = "MARKER_IDENTITY_NOTE"
        const val MARKER_SPEECH_ARCHETYPE_NOTE = "MARKER_SPEECH_ARCHETYPE_NOTE"
        const val MARKER_RECOGNIZABILITY = "MARKER_RECOGNIZABILITY"
        const val MARKER_LONG_TERM_VIABILITY = "MARKER_LONG_TERM_VIABILITY"
        const val MARKER_IMBALANCE_NOTE = "MARKER_IMBALANCE_NOTE"
        const val MARKER_TIE_TO_LIE_NON_NEGOTIABLE = "MARKER_TIE_TO_LIE_NON_NEGOTIABLE"
        const val MARKER_TIE_TO_LIE_FORBIDDEN = "MARKER_TIE_TO_LIE_FORBIDDEN"
        const val MARKER_TIE_TO_LIE_RELATION = "MARKER_TIE_TO_LIE_RELATION"
        const val MARKER_RELATION_DESCRIPTION = "MARKER_RELATION_DESCRIPTION"
        const val MARKER_VOICE_SAMPLE_DEMONSTRATES = "MARKER_VOICE_SAMPLE_DEMONSTRATES"

        val EXCLUDED_MARKERS = listOf(
            MARKER_ONE_LINE,
            MARKER_NEED,
            MARKER_WANT_NEED_CONFLICT,
            MARKER_EXPRESSION_NOTE,
            MARKER_IDENTITY_NOTE,
            MARKER_SPEECH_ARCHETYPE_NOTE,
            MARKER_RECOGNIZABILITY,
            MARKER_LONG_TERM_VIABILITY,
            MARKER_IMBALANCE_NOTE,
            MARKER_TIE_TO_LIE_NON_NEGOTIABLE,
            MARKER_TIE_TO_LIE_FORBIDDEN,
            MARKER_TIE_TO_LIE_RELATION,
            MARKER_RELATION_DESCRIPTION,
            MARKER_VOICE_SAMPLE_DEMONSTRATES,
        )
    }
}
