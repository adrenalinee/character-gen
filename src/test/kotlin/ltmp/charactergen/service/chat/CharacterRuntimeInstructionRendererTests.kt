package ltmp.charactergen.service.chat

import ltmp.charactergen.config.L0_RUNTIME_INSTRUCTION_V2
import ltmp.charactergen.data.BannedWordSource
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.data.RelationshipStage
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.CharacterBannedWordEntity
import ltmp.charactergen.data.entity.CharacterBehavioralRuleEntity
import ltmp.charactergen.data.entity.CharacterConcreteDetailEntity
import ltmp.charactergen.data.entity.CharacterDisclosureItemEntity
import ltmp.charactergen.data.entity.CharacterForbiddenBehaviorEntity
import ltmp.charactergen.data.entity.CharacterNonNegotiableEntity
import ltmp.charactergen.data.entity.CharacterRelationEntity
import ltmp.charactergen.data.entity.CharacterRelationshipStageEntity
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
 * L0 런타임 주입 설계 1절의 필드 라우팅과 3절의 관계 단계 분기, 생성 프롬프트 v4 §8.1의 소비처 구분이
 * 실제로 지켜지는지 검증한다. 여기가 깨지면 이후 프로브 수치는 의미를 잃는다.
 */
class CharacterRuntimeInstructionRendererTests {
    private val renderer = CharacterRuntimeInstructionRenderer()

    @Test
    fun `설계 근거와 요약 필드는 인스트럭션에 주입되지 않는다`() {
        val instruction = render(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST)

        // 설계 1절과 v4 소비처 매트릭스에서 '제외'로 분류한 필드들
        EXCLUDED_MARKERS.forEach { marker ->
            assertFalse(instruction.contains(marker), "제외되어야 할 값이 주입되었다: $marker")
        }
    }

    @Test
    fun `상시 주입 필드는 모두 인스트럭션에 들어간다`() {
        val instruction = render(RelationshipStage.EARLY)

        listOf(
            "서윤",
            "웹툰 작가",
            "밤에만 작업한다",
            "칭찬을 받으면 다음 작업 이야기로 넘긴다",
            "누가 자기 몫을 대신하려 하면 먼저 손을 뺀다",
            "필요한 말만 한다",
            "짧은 결론 뒤 비유",
            "거절할 때",
            "선을 다시 긋는다고 말한다",
            "문장이 더 짧아진다",
            "감정을 사실로 바꾼다",
            "한 문장 뒤 침묵한다",
            "먼저 선을 긋는다",
            "떠나면 붙잡는다",
            "비교하지 않는다",
            "상대를 시험하지 않는다",
            "자기 방식의 작품을 남긴다",
            "민호 — 전 편집자",
        ).forEach { expected ->
            assertTrue(instruction.contains(expected), "상시 주입 값이 빠졌다: $expected")
        }
    }

    @Test
    fun `금지어는 두 출처를 병합해 한 줄로 넣는다`() {
        val instruction = render(RelationshipStage.EARLY)

        assertTrue(instruction.contains("쓰지 않는 말: 무조건, 당연히"))
    }

    @Test
    fun `구체 묘사는 세 개까지만 넣는다`() {
        val instruction = render(RelationshipStage.EARLY)

        assertTrue(instruction.contains("묘사1"))
        assertTrue(instruction.contains("묘사3"))
        assertFalse(instruction.contains("묘사4"), "구체 묘사는 3개를 넘기지 않아야 한다")
    }

    @Test
    fun `관계 단계에 해당하는 단계 서술 한 벌만 넣는다`() {
        val early = render(RelationshipStage.EARLY)
        assertTrue(early.contains("먼저 묻지 않는다"))
        assertTrue(early.contains("호칭: 이름 없이 존댓말로 부른다"))
        assertTrue(early.contains("감정: EARLY 단계의 감정 범위"))
        assertTrue(early.contains("회피: EARLY 단계의 회피 강도"))
        assertFalse(early.contains("작업 이야기만 연다"))
        assertFalse(early.contains("거리를 유지한 채 곁에 둔다"))

        val deepening = render(RelationshipStage.DEEPENING)
        assertTrue(deepening.contains("작업 이야기만 연다"))
        assertTrue(deepening.contains("호칭: 이름을 부르되 존댓말을 유지한다"))
        assertFalse(deepening.contains("먼저 묻지 않는다"))

        val close = render(RelationshipStage.CLOSE)
        assertTrue(close.contains("거리를 유지한 채 곁에 둔다"))
        assertTrue(close.contains("호칭: 이름을 줄여 부르고 반말을 섞는다"))
        assertFalse(close.contains("먼저 묻지 않는다"))
    }

    @Test
    fun `단계 서술과 개방 층위는 서로 독립이다`() {
        // 개방을 넓혀도 단계 서술은 그대로고, 좁혀도 마찬가지다
        val earlyWideOpen = render(RelationshipStage.EARLY, DisclosureLevel.DEEPEST)
        assertTrue(earlyWideOpen.contains("먼저 묻지 않는다"))
        assertTrue(earlyWideOpen.contains("자기 가치에 대한 공포"))

        val closeButShallow = render(RelationshipStage.CLOSE, DisclosureLevel.SURFACE)
        assertTrue(closeButShallow.contains("거리를 유지한 채 곁에 둔다"))
        assertFalse(closeButShallow.contains("교체 경험"))
    }

    @Test
    fun `관계 사건 반응은 단계와 무관하게 항상 넣는다`() {
        // 거절·마찰·오랜 부재·과속 접근은 어느 단계에서든 일어난다
        RelationshipStage.entries.forEach { stage ->
            val instruction = render(stage)
            listOf(
                "한 번 물러난 뒤 다시 묻지 않는다",
                "근거를 한 줄로 말하고 물러서지 않는다",
                "없었던 일처럼 작업 이야기부터 잇는다",
                "속도를 늦추고 하던 이야기로 돌아온다",
            ).forEach { expected ->
                assertTrue(instruction.contains(expected), "관계 사건 반응이 빠졌다: $stage / $expected")
            }
        }
    }

    @Test
    fun `열리지 않은 층위의 개인사 본문은 주입되지 않는다`() {
        val early = render(RelationshipStage.EARLY)
        assertTrue(early.contains("마감 습관"))
        assertFalse(early.contains("편집자 이야기"))
        assertFalse(early.contains("교체 경험"))
        assertFalse(early.contains("자기 가치에 대한 공포"))

        // 가까운 관계라도 deepest는 저절로 열리지 않는다
        val close = render(RelationshipStage.CLOSE)
        assertTrue(close.contains("교체 경험"))
        assertFalse(close.contains("자기 가치에 대한 공포"))

        val deepest = render(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST)
        assertTrue(deepest.contains("자기 가치에 대한 공포"))
    }

    @Test
    fun `닫힌 주제는 방식 A에서 통째로 빠지고 방식 B에서는 주제 이름만 남는다`() {
        val topicOnly = render(RelationshipStage.EARLY, closedTopicMode = ClosedTopicMode.TOPIC_ONLY)
        assertTrue(topicOnly.contains("## 말하지 않는 것"))
        assertTrue(topicOnly.contains("쉽게 말하지 않는 과거"))
        assertFalse(topicOnly.contains("교체 경험"), "주제 이름만 넣어야 하는데 본문이 샜다")

        val omit = render(RelationshipStage.EARLY, closedTopicMode = ClosedTopicMode.OMIT)
        assertFalse(omit.contains("## 말하지 않는 것"), "방식 A에서는 헤더도 남으면 안 된다")
        assertFalse(omit.contains("쉽게 말하지 않는 과거"))
    }

    @Test
    fun `모든 층위가 열리면 닫힌 주제 구간이 사라진다`() {
        val fullyOpen = render(
            RelationshipStage.CLOSE,
            DisclosureLevel.DEEPEST,
            closedTopicMode = ClosedTopicMode.TOPIC_ONLY,
        )

        assertFalse(fullyOpen.contains("## 말하지 않는 것"))
    }

    @Test
    fun `예시 발화를 끄면 헤더까지 사라진다`() {
        val included = render(RelationshipStage.EARLY, includeVoiceSamples = true)
        assertTrue(included.contains("## 톤 참조"))
        assertTrue(included.contains("그 선은 제가 정할게요."))
        assertTrue(included.contains("위 문장을 그대로 쓰지 마라"))

        val excluded = render(RelationshipStage.EARLY, includeVoiceSamples = false)
        assertFalse(excluded.contains("## 톤 참조"), "구간을 비울 때 헤더가 남으면 안 된다")
        assertFalse(excluded.contains("그 선은 제가 정할게요."))
    }

    @Test
    fun `상황을 주지 않으면 상황 구간이 사라진다`() {
        val withSituation = render(RelationshipStage.EARLY, situationNote = "오늘 원고를 크게 반려당했다")
        assertTrue(withSituation.contains("## 지금 네 상황"))
        assertTrue(withSituation.contains("오늘 원고를 크게 반려당했다"))

        val withoutSituation = render(RelationshipStage.EARLY, situationNote = null)
        assertFalse(withoutSituation.contains("## 지금 네 상황"))
    }

    @Test
    fun `상처는 심층 층위가 열렸을 때만 주입된다`() {
        assertFalse(render(RelationshipStage.EARLY).contains("## 잘 꺼내지 않는 과거"))
        assertFalse(render(RelationshipStage.EARLY).contains("첫 연재에서 교체됐다"))
        assertFalse(render(RelationshipStage.DEEPENING).contains("첫 연재에서 교체됐다"))
        assertTrue(render(RelationshipStage.CLOSE).contains("## 잘 꺼내지 않는 과거"))
        assertTrue(render(RelationshipStage.CLOSE).contains("첫 연재에서 교체됐다"))
        // 단계가 아니라 개방 층위가 기준이다
        assertTrue(render(RelationshipStage.EARLY, DisclosureLevel.DEEP).contains("첫 연재에서 교체됐다"))
        assertFalse(render(RelationshipStage.CLOSE, DisclosureLevel.MID).contains("첫 연재에서 교체됐다"))
    }

    @Test
    fun `치환되지 않은 placeholder와 빈 줄이 남지 않는다`() {
        RelationshipStage.entries.forEach { stage ->
            DisclosureLevel.entries.forEach { level ->
                listOf(ClosedTopicMode.OMIT, ClosedTopicMode.TOPIC_ONLY).forEach { mode ->
                    listOf(true, false).forEach { includeSamples ->
                        val instruction = render(stage, level, mode, includeSamples, situationNote = null)
                        assertFalse(
                            instruction.contains("{{"),
                            "치환되지 않은 placeholder가 남았다: $stage/$level/$mode/$includeSamples",
                        )
                        assertFalse(
                            instruction.contains("\n\n\n"),
                            "빈 구간이 연속 빈 줄을 남겼다: $stage/$level/$mode/$includeSamples",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `조립된 인스트럭션은 머리말과 캐릭터 선언으로 시작한다`() {
        assertEquals(
            listOf("# CHARACTER_PROMPT", "너는 서윤이다. 아래는 너 자신에 대한 것이며, 사용자에게 설명하지 않는다."),
            render(RelationshipStage.EARLY).lineSequence().take(2).toList(),
        )
    }

    /** 개방 층위를 주지 않으면 운영 기본값과 같게 단계에서 파생한다. */
    private fun render(
        stage: RelationshipStage,
        openedDisclosureLevel: DisclosureLevel = stage.defaultOpenedLevel,
        closedTopicMode: ClosedTopicMode = ClosedTopicMode.TOPIC_ONLY,
        includeVoiceSamples: Boolean = true,
        situationNote: String? = null,
    ): String = renderer.render(
        L0_RUNTIME_INSTRUCTION_V2,
        character(),
        RuntimeInstructionOptions(
            relationshipStage = stage,
            openedDisclosureLevel = openedDisclosureLevel,
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
        lieStatement = MARKER_LIE_STATEMENT,
        lieWound = "첫 연재에서 교체됐다",
        want = "자기 방식의 작품을 남긴다",
        need = MARKER_NEED,
        wantNeedConflict = MARKER_WANT_NEED_CONFLICT,
        contradictionTraitA = "먼저 선을 긋는다",
        contradictionTraitB = "떠나면 붙잡는다",
        contradictionSharedRoot = MARKER_SHARED_ROOT,
        expressionNote = "필요한 말만 한다",
        identityNote = MARKER_IDENTITY_NOTE,
        onRejection = "한 번 물러난 뒤 다시 묻지 않는다",
        onConflict = "근거를 한 줄로 말하고 물러서지 않는다",
        onAbsence = "없었던 일처럼 작업 이야기부터 잇는다",
        onOverApproach = "속도를 늦추고 하던 이야기로 돌아온다",
        emotionalExpressionNote = "감정을 사실로 바꾼다",
        avoidanceNote = "한 문장 뒤 침묵한다",
        speechArchetypeNote = MARKER_SPEECH_ARCHETYPE_NOTE,
        sentencePattern = "짧은 결론 뒤 비유",
        underStress = "문장이 더 짧아진다",
        recognizability = MARKER_RECOGNIZABILITY,
        longTermViability = MARKER_LONG_TERM_VIABILITY,
        imbalanceNote = MARKER_IMBALANCE_NOTE,
        concreteDetails = (1..4).map { CharacterConcreteDetailEntity(detailText = "묘사$it") },
        behavioralRules = listOf(
            CharacterBehavioralRuleEntity(ruleText = "칭찬을 받으면 다음 작업 이야기로 넘긴다"),
            CharacterBehavioralRuleEntity(ruleText = "누가 자기 몫을 대신하려 하면 먼저 손을 뺀다"),
        ),
        relationshipStages = listOf(
            relationshipStage(RelationshipStage.EARLY, "먼저 묻지 않는다", "이름 없이 존댓말로 부른다"),
            relationshipStage(RelationshipStage.DEEPENING, "작업 이야기만 연다", "이름을 부르되 존댓말을 유지한다"),
            relationshipStage(RelationshipStage.CLOSE, "거리를 유지한 채 곁에 둔다", "이름을 줄여 부르고 반말을 섞는다"),
        ),
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

    private fun relationshipStage(
        stage: RelationshipStage,
        attachment: String,
        address: String,
    ) = CharacterRelationshipStageEntity(
        stage = stage,
        attachment = attachment,
        address = address,
        emotionalRange = "${stage.name} 단계의 감정 범위",
        avoidance = "${stage.name} 단계의 회피 강도",
    )

    private companion object {
        const val MARKER_ONE_LINE = "MARKER_ONE_LINE"
        const val MARKER_LIE_STATEMENT = "MARKER_LIE_STATEMENT"
        const val MARKER_SHARED_ROOT = "MARKER_SHARED_ROOT"
        const val MARKER_NEED = "MARKER_NEED"
        const val MARKER_WANT_NEED_CONFLICT = "MARKER_WANT_NEED_CONFLICT"
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
            MARKER_LIE_STATEMENT,
            MARKER_SHARED_ROOT,
            MARKER_NEED,
            MARKER_WANT_NEED_CONFLICT,
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
