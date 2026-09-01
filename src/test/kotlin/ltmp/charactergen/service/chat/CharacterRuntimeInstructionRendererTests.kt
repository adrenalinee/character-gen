package ltmp.charactergen.service.chat

import ltmp.charactergen.config.L0_RUNTIME_INSTRUCTION_V6
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
 * L0 런타임 주입 설계 1절의 필드 라우팅과 3절의 관계 단계 분기, 생성 프롬프트 v4.2 C-6의 소비처 구분이
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
            "짧은 결론 뒤 비유",
            "거절할 때",
            "선을 다시 긋는다고 말한다",
            "감정을 사실로 바꾼다",
            "한 문장 뒤 침묵한다",
            "먼저 선을 긋는다",
            "떠나면 붙잡는다",
            "비교하지 않는다",
            "상대를 시험하지 않는다",
            "자기 방식의 작품을 남긴다",
            "현존1 — 동료1",
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
    fun `런타임 대상으로 표시된 구체 묘사와 말버릇만 넣는다`() {
        val instruction = render(RelationshipStage.EARLY)

        assertTrue(instruction.contains("묘사1"))
        assertTrue(instruction.contains("묘사3"))
        assertFalse(
            instruction.contains("묘사4"),
            "runtimeIncluded가 false인 묘사는 주입되지 않아야 한다",
        )
        assertFalse(
            instruction.contains(MARKER_EXCLUDED_VERBAL_TIC),
            "runtimeIncluded가 false인 말버릇은 주입되지 않아야 한다",
        )
    }

    @Test
    fun `겹친다고 판정된 문장 패턴은 라벨째로 빠진다`() {
        // 라벨만 남으면 "문장:" 뒤가 빈 줄이 된다. (v4.2 B-13)
        val included = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(),
            options(RelationshipStage.EARLY),
        )
        assertTrue(included.contains("문장: 짧은 결론 뒤 비유"))

        val excluded = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(sentencePatternRuntimeIncluded = false),
            options(RelationshipStage.EARLY),
        )
        assertFalse(excluded.contains("짧은 결론 뒤 비유"))
        assertFalse(excluded.contains("문장:"), "값이 빠지면 라벨도 남으면 안 된다")
    }

    @Test
    fun `개방된 항목은 surface와 다른 섹션에 넣는다`() {
        // 한 목록에 섞이면 층위 구분이 사라진다. (v4.2 B-12)
        val early = render(RelationshipStage.EARLY)
        val surfaceSection = early.substringAfter("## 말할 수 있는 것").substringBefore("##")
        assertTrue(surfaceSection.contains("마감 습관"))
        assertFalse(surfaceSection.contains("편집자 이야기"), "개방 항목이 surface 목록에 섞였다")

        assertTrue(early.contains("## 가까워지면서 말하게 된 것"))
        assertTrue(
            early.substringAfter("## 가까워지면서 말하게 된 것").contains("편집자 이야기"),
        )
        assertTrue(early.contains("위에 없는 개인사는 지어내지 않는다"))
    }

    @Test
    fun `중간 층위와 심층 층위는 서로 다른 섹션에 넣는다`() {
        // 층위가 섞이면 캐릭터가 오래 걸려 여는 이야기에 무게를 실을 수 없다. (스펙 v1.1 Q-3)
        val deepening = render(RelationshipStage.DEEPENING)
        assertTrue(deepening.contains("## 가까워지면서 말하게 된 것"))
        assertFalse(deepening.contains("## 오래 알고 나서야 말한 것"), "mid만 열렸는데 심층 섹션이 나왔다")

        val close = render(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST)
        val midSection = close.substringAfter("## 가까워지면서 말하게 된 것")
            .substringBefore("## 오래 알고 나서야 말한 것")
        val deepSection = close.substringAfter("## 오래 알고 나서야 말한 것").substringBefore("위에 없는 개인사")

        assertTrue(midSection.contains("편집자 이야기"))
        assertFalse(midSection.contains("교체 경험"), "심층 항목이 중간 층위 목록에 섞였다")
        assertTrue(deepSection.contains("교체 경험"))
        assertTrue(deepSection.contains("자기 가치에 대한 공포"))
    }

    @Test
    fun `상처와 같은 사건을 되풀이하는 개방 항목은 빠지고 상처는 남는다`() {
        // 생성 단계가 1차, 이것은 조립 단계 백스톱이다. 겹쳤을 때 남기는 쪽은 상처다. (스펙 v1.1 Q-3)
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(
                lieWound = "첫 연재에서 편집자가 원고를 반려하고 다른 작가로 교체했다",
                deepDisclosureText = "그 연재에서 편집자가 원고를 반려한 뒤로 마감을 앞당겼다",
            ),
            options(RelationshipStage.CLOSE),
        )

        assertFalse(instruction.contains("마감을 앞당겼다"), "상처와 같은 사건을 말하는 deep 항목이 남았다")
        assertTrue(instruction.contains("## 잘 꺼내지 않는 과거"))
        assertTrue(instruction.contains("다른 작가로 교체했다"))
    }

    @Test
    fun `상처 가드는 한 번에 다 말하지 말라는 문장까지 포함한다`() {
        // wound는 한 문장으로 압축되어 있어 열리는 순간 통째로 발화되기 쉽다. (스펙 v1.1 Q-2)
        val close = render(RelationshipStage.CLOSE)

        assertTrue(close.contains("이 일을 먼저 꺼내지 않는다. 깊이 물어올 때만 조금씩 드러낸다."))
        assertTrue(close.contains("한 번에 전부 말하지 않는다."))
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
    fun `얕은 관계에서도 중간 층위 한 항목은 열어 둔다`() {
        // 초기 대화의 소재가 surface만으로는 금방 바닥나므로 임시로 한 항목을 연다. (v4.1 D-2 C안)
        val early = render(RelationshipStage.EARLY)
        assertTrue(early.contains("편집자 이야기"), "early에서 mid 첫 항목이 열려야 한다")
        assertFalse(early.contains("편집자와 틀어진 일"), "mid 두 번째 항목까지 열리면 안 된다")

        // 이미 mid까지 열린 단계에서는 따로 더 붙이지 않는다.
        val deepening = render(RelationshipStage.DEEPENING)
        assertTrue(deepening.contains("편집자 이야기"))
        assertTrue(deepening.contains("편집자와 틀어진 일"))
    }

    @Test
    fun `지금 만나지 않는 사람은 심층이 열렸을 때만 따로 표시한다`() {
        // presence가 '부재'인 인물을 현존 인물과 같은 줄에 나열하면 요즘도 만나는 사람처럼 다루게 된다. (v4.1 B-9)
        val early = render(RelationshipStage.EARLY)
        assertFalse(early.contains("민호"), "부재 인물은 주변 사람 목록에 들어가면 안 된다")
        assertFalse(early.contains("지금은 만나지 않는 사람"))

        val close = render(RelationshipStage.CLOSE)
        assertTrue(close.contains("## 지금은 만나지 않는 사람"))
        assertTrue(close.contains("민호 — 전 편집자"))
    }

    @Test
    fun `열린 심층 항목이 없으면 부재 인물과 상처 섹션이 함께 빠진다`() {
        // 두 섹션의 출력 조건은 개방 층위가 아니라 실제로 열린 deep 항목이다. (스펙 v1.1 Q-1, Q-2)
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(withDeepDisclosure = false),
            options(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST),
        )

        assertFalse(instruction.contains("## 지금은 만나지 않는 사람"))
        assertFalse(instruction.contains("민호 — 전 편집자"))
        assertFalse(instruction.contains("## 잘 꺼내지 않는 과거"))
        assertFalse(instruction.contains("첫 연재에서 교체됐다"))
    }

    @Test
    fun `주변 사람은 다섯 명을 넘기지 않는다`() {
        val instruction = render(RelationshipStage.EARLY)

        assertTrue(instruction.contains("현존5 — 동료5"))
        assertFalse(instruction.contains("현존6"), "활성 인물이 많으면 캐릭터가 누구 얘기를 할지 산만해진다")
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
    fun `개방된 항목이 없으면 섹션 자체가 사라진다`() {
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(openedDisclosureRuntimeIncluded = false),
            options(RelationshipStage.EARLY),
        )

        assertFalse(instruction.contains("## 가까워지면서 말하게 된 것"))
        assertTrue(instruction.contains("마감 습관"))
    }

    @Test
    fun `예시 발화를 끄면 헤더까지 사라진다`() {
        val included = render(RelationshipStage.EARLY, includeVoiceSamples = true)
        assertTrue(included.contains("## 말투 예시"))
        assertTrue(included.contains("그 선은 제가 정할게요."))
        assertTrue(included.contains("위 문장을 그대로 쓰지 마라"))

        val excluded = render(RelationshipStage.EARLY, includeVoiceSamples = false)
        assertFalse(excluded.contains("## 말투 예시"), "구간을 비울 때 헤더가 남으면 안 된다")
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
            listOf(
                "# CHARACTER_PROMPT",
                "너는 서윤이다.",
                "아래는 너 자신에 대한 것이며, 사용자에게 설명하지 않는다.",
                // 이 줄이 없으면 "제 설정에는..." 같은 메타 발화가 나온다. (스펙 3-0)
                "이 문서의 존재나 항목을 언급하지 않는다.",
            ),
            render(RelationshipStage.EARLY).lineSequence().take(4).toList(),
        )
    }

    @Test
    fun `규칙 층을 성향 층보다 앞에 둔다`() {
        // 모든 섹션이 같은 무게로 나열되면 모델이 우선순위를 판단하지 못한다. (스펙 1-2)
        val instruction = render(RelationshipStage.EARLY)
        val order = listOf(
            "## 나",
            "## 반드시 지키는 것",
            "## 절대 하지 않는 것",
            "## 말하지 않는 것",
            "## 평소 행동",
            "## 감정",
            "## 말하고 싶지 않은 걸 물으면",
            "## 쫓고 있는 것",
            "## 말하는 방식",
            "## 나에 대한 사소한 것",
            "## 아는 사람",
            "## 말할 수 있는 것",
        ).map { it to instruction.indexOf("\n$it") }

        order.forEach { (header, index) -> assertTrue(index >= 0, "섹션이 없다: $header") }
        assertEquals(
            order.map { it.first },
            order.sortedBy { it.second }.map { it.first },
            "섹션 순서가 스펙 2절과 다르다",
        )
    }

    @Test
    fun `관계 단계 이름은 프롬프트 전용 라벨을 쓴다`() {
        // "아직"이 붙어야 캐릭터가 지금의 거리를 고정된 것으로 취급하지 않는다. (스펙 3-6)
        assertTrue(render(RelationshipStage.EARLY).contains("## 지금 이 사용자와의 관계: 아직 얕은 관계"))
        assertTrue(render(RelationshipStage.DEEPENING).contains("## 지금 이 사용자와의 관계: 가까워지는 중"))
        assertTrue(render(RelationshipStage.CLOSE).contains("## 지금 이 사용자와의 관계: 가까운 관계"))
    }

    @Test
    fun `직업의 핵심 명사구가 일상 서술에 이미 있으면 일상 줄을 빼다`() {
        // 같은 말이 두 줄에 걸쳐 반복되면 모델이 그 소재만 붙들고 이야기한다. (스펙 3-1)
        val repeated = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(occupation = "생활 다큐멘터리 사진가", dailyLife = "생활 다큐멘터리 촬영과 암실 작업을 오간다"),
            options(RelationshipStage.EARLY),
        )
        assertTrue(repeated.contains("29세. 생활 다큐멘터리 사진가"))
        assertFalse(repeated.contains("암실 작업"), "겹치는 일상 서술이 남았다")

        // 겹치지 않으면 두 줄 모두 남는다.
        val distinct = render(RelationshipStage.EARLY)
        assertTrue(distinct.contains("29세. 웹툰 작가"))
        assertTrue(distinct.contains("밤에만 작업한다"))
    }

    @Test
    fun `내용이 없는 섹션은 제목과 가드까지 사라진다`() {
        // 제목만 남으면 모델이 그 자리를 채우려 한다. (스펙 1-5)
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(withOptionalCollections = false),
            options(RelationshipStage.EARLY),
        )

        assertFalse(instruction.contains("## 아는 사람"))
        assertFalse(instruction.contains("이 사람들에 대해서는 이름과 관계 정도만 말한다"))
        assertFalse(instruction.contains("## 나에 대한 사소한 것"))
        assertFalse(instruction.contains("상황별로:"))
        assertFalse(instruction.contains("쓰지 않는 말:"))
        assertFalse(instruction.contains("## 말투 예시"))
        // 규칙 층은 그대로 남아 있어야 한다.
        assertTrue(instruction.contains("## 반드시 지키는 것"))
        assertTrue(instruction.contains("## 절대 하지 않는 것"))
    }

    @Test
    fun `아는 사람 목록에는 관계 서사 금지 가드가 붙는다`() {
        // relations의 description은 주입하지 않으므로 캐릭터는 이들에 대해 할 말이 없어야 정상이다. (스펙 3-5)
        val instruction = render(RelationshipStage.EARLY)

        assertTrue(instruction.contains("## 아는 사람"))
        assertTrue(instruction.contains("이 사람들에 대해서는 이름과 관계 정도만 말한다."))
        assertTrue(instruction.contains("더 자세한 이야기는 아직 이 사용자에게 하지 않는다."))
    }

    @Test
    fun `공개 범위 항목 수는 관계 단계별 예산을 넘기지 않는다`() {
        // 단계가 올라갈수록 개방 항목이 쌓이므로 상한도 단계별로 다르다. (스펙 v1.1 Q-4)
        assertEquals(12, disclosureItemCount(RelationshipStage.DEEPENING, DisclosureLevel.MID))
        assertEquals(16, disclosureItemCount(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST))
    }

    @Test
    fun `예산이 모자라도 처음 만난 사람에게 할 이야기는 남긴다`() {
        // surface를 통째로 비우면 "꺼낼 수 있는 개인사가 없다"가 나온다. 예산 때문에 이번에 빼는 것과 뜻이 다르다.
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(extraMidDisclosureCount = 20),
            options(RelationshipStage.CLOSE, DisclosureLevel.DEEPEST),
        )

        assertTrue(instruction.contains("마감 습관"))
        assertFalse(instruction.contains("꺼낼 수 있는 개인사가 없다"))
    }

    /** 공개 범위 세 섹션에 실제로 들어간 항목 수. 예산은 제목과 가드가 아니라 항목 줄만 센다. */
    private fun disclosureItemCount(stage: RelationshipStage, openedLevel: DisclosureLevel): Int {
        val instruction = renderer.render(
            L0_RUNTIME_INSTRUCTION_V6,
            character(extraMidDisclosureCount = 20),
            options(stage, openedLevel),
        )
        return instruction.substringAfter("## 말할 수 있는 것")
            .substringBefore("위에 없는 개인사")
            .lines()
            .count { it.startsWith("- ") }
    }

    /** 개방 층위를 주지 않으면 운영 기본값과 같게 단계에서 파생한다. */
    private fun render(
        stage: RelationshipStage,
        openedDisclosureLevel: DisclosureLevel = stage.defaultOpenedLevel,
        closedTopicMode: ClosedTopicMode = ClosedTopicMode.TOPIC_ONLY,
        includeVoiceSamples: Boolean = true,
        situationNote: String? = null,
    ): String = renderer.render(
        L0_RUNTIME_INSTRUCTION_V6,
        character(),
        options(stage, openedDisclosureLevel, closedTopicMode, includeVoiceSamples, situationNote),
    )

    private fun options(
        stage: RelationshipStage,
        openedDisclosureLevel: DisclosureLevel = stage.defaultOpenedLevel,
        closedTopicMode: ClosedTopicMode = ClosedTopicMode.TOPIC_ONLY,
        includeVoiceSamples: Boolean = true,
        situationNote: String? = null,
    ) = RuntimeInstructionOptions(
        relationshipStage = stage,
        openedDisclosureLevel = openedDisclosureLevel,
        situationNote = situationNote,
        includeVoiceSamples = includeVoiceSamples,
        closedTopicMode = closedTopicMode,
    )

    /**
     * @param withOptionalCollections false면 비어도 되는 컬렉션을 모두 비운다.
     *   내용이 없는 섹션이 제목까지 사라지는지 보기 위한 것이다.
     * @param extraMidDisclosureCount 개방 항목 상한을 넘기기 위해 더 넣는 mid 항목 수.
     * @param withDeepDisclosure false면 deep 항목을 비운다. 부재 인물과 상처 섹션의 출력 조건을 보기 위한 것이다.
     */
    private fun character(
        sentencePatternRuntimeIncluded: Boolean = true,
        openedDisclosureRuntimeIncluded: Boolean = true,
        occupation: String = "웹툰 작가",
        dailyLife: String = "밤에만 작업한다",
        withOptionalCollections: Boolean = true,
        extraMidDisclosureCount: Int = 0,
        withDeepDisclosure: Boolean = true,
        lieWound: String = "첫 연재에서 교체됐다",
        deepDisclosureText: String = "교체 경험",
    ): Character = Character(
        generationExecute = mock(L0GenerationExecute::class.java),
        name = "서윤",
        age = 29,
        gender = "여성",
        occupation = occupation,
        dailyLife = dailyLife,
        oneLine = MARKER_ONE_LINE,
        lieType = "대체 가능성 공포형",
        lieStatement = MARKER_LIE_STATEMENT,
        lieWound = lieWound,
        want = "자기 방식의 작품을 남긴다",
        need = MARKER_NEED,
        wantNeedConflict = MARKER_WANT_NEED_CONFLICT,
        contradictionTraitA = "먼저 선을 긋는다",
        contradictionTraitB = "떠나면 붙잡는다",
        contradictionSharedRoot = MARKER_SHARED_ROOT,
        onRejection = "한 번 물러난 뒤 다시 묻지 않는다",
        onConflict = "근거를 한 줄로 말하고 물러서지 않는다",
        onAbsence = "없었던 일처럼 작업 이야기부터 잇는다",
        onOverApproach = "속도를 늦추고 하던 이야기로 돌아온다",
        emotionalExpressionNote = "감정을 사실로 바꾼다",
        avoidanceNote = "한 문장 뒤 침묵한다",
        speechArchetypeNote = MARKER_SPEECH_ARCHETYPE_NOTE,
        sentencePattern = "짧은 결론 뒤 비유",
        sentencePatternRuntimeIncluded = sentencePatternRuntimeIncluded,
        recognizability = MARKER_RECOGNIZABILITY,
        longTermViability = MARKER_LONG_TERM_VIABILITY,
        imbalanceNote = MARKER_IMBALANCE_NOTE,
        traitReflection = MARKER_TRAIT_REFLECTION,
        // 묘사4는 저장 시점에 다른 대화용 필드와 겹친다고 판정돼 런타임에서 빠진 항목이다. (v4.2 B-13)
        concreteDetails = if (!withOptionalCollections) {
            emptyList()
        } else {
            (1..4).map { CharacterConcreteDetailEntity(detailText = "묘사$it", runtimeIncluded = it <= 3) }
        },
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
        // 두 번째 말버릇은 겹침으로 판정돼 런타임에서 빠진 항목이다. (v4.2 B-13)
        verbalTics = if (!withOptionalCollections) emptyList() else listOf(
            CharacterVerbalTicEntity(
                situation = "거절할 때",
                description = "선을 다시 긋는다고 말한다",
                runtimeIncluded = true,
            ),
            CharacterVerbalTicEntity(
                situation = "칭찬을 들을 때",
                description = MARKER_EXCLUDED_VERBAL_TIC,
                runtimeIncluded = false,
            ),
        ),
        bannedWords = if (!withOptionalCollections) {
            emptyList()
        } else {
            listOf(
                CharacterBannedWordEntity(source = BannedWordSource.FROM_LIE, wordText = "무조건"),
                CharacterBannedWordEntity(source = BannedWordSource.FROM_BACKGROUND, wordText = "당연히"),
            )
        },
        relations = if (!withOptionalCollections) emptyList() else listOf(
            CharacterRelationEntity(
                slot = RelationSlot.`과거`,
                name = "민호",
                relation = "전 편집자",
                description = MARKER_RELATION_DESCRIPTION,
                presence = RelationPresence.`부재`,
                tieToLie = MARKER_TIE_TO_LIE_RELATION,
            ),
        ) + (1..6).map {
            CharacterRelationEntity(
                slot = RelationSlot.`조력자`,
                name = "현존$it",
                relation = "동료$it",
                description = MARKER_RELATION_DESCRIPTION,
                presence = RelationPresence.`현존`,
                tieToLie = MARKER_TIE_TO_LIE_RELATION,
            )
        },
        disclosureItems = listOf(
            CharacterDisclosureItemEntity(DisclosureLevel.SURFACE, "마감 습관", runtimeIncluded = true),
            CharacterDisclosureItemEntity(DisclosureLevel.MID, "편집자 이야기", openedDisclosureRuntimeIncluded),
            CharacterDisclosureItemEntity(DisclosureLevel.MID, "편집자와 틀어진 일", openedDisclosureRuntimeIncluded),
        ) + deepDisclosureItems(withDeepDisclosure, deepDisclosureText, openedDisclosureRuntimeIncluded) +
            (1..extraMidDisclosureCount).map {
                CharacterDisclosureItemEntity(DisclosureLevel.MID, "추가 중간 항목$it", runtimeIncluded = true)
            },
        voiceSamples = if (!withOptionalCollections) emptyList() else listOf(
            CharacterVoiceSampleEntity(
                situation = "의견에 반대했을 때",
                line = "그 선은 제가 정할게요.",
                demonstrates = MARKER_VOICE_SAMPLE_DEMONSTRATES,
            ),
        ),
    )

    private fun deepDisclosureItems(
        withDeepDisclosure: Boolean,
        deepDisclosureText: String,
        runtimeIncluded: Boolean,
    ): List<CharacterDisclosureItemEntity> {
        if (!withDeepDisclosure) return emptyList()
        return listOf(
            CharacterDisclosureItemEntity(DisclosureLevel.DEEP, deepDisclosureText, runtimeIncluded),
            CharacterDisclosureItemEntity(DisclosureLevel.DEEPEST, "자기 가치에 대한 공포", runtimeIncluded),
        )
    }

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
        const val MARKER_TRAIT_REFLECTION = "MARKER_TRAIT_REFLECTION"
        const val MARKER_SPEECH_ARCHETYPE_NOTE = "MARKER_SPEECH_ARCHETYPE_NOTE"
        const val MARKER_RECOGNIZABILITY = "MARKER_RECOGNIZABILITY"
        const val MARKER_LONG_TERM_VIABILITY = "MARKER_LONG_TERM_VIABILITY"
        const val MARKER_IMBALANCE_NOTE = "MARKER_IMBALANCE_NOTE"
        const val MARKER_TIE_TO_LIE_NON_NEGOTIABLE = "MARKER_TIE_TO_LIE_NON_NEGOTIABLE"
        const val MARKER_TIE_TO_LIE_FORBIDDEN = "MARKER_TIE_TO_LIE_FORBIDDEN"
        const val MARKER_TIE_TO_LIE_RELATION = "MARKER_TIE_TO_LIE_RELATION"
        const val MARKER_RELATION_DESCRIPTION = "MARKER_RELATION_DESCRIPTION"
        const val MARKER_VOICE_SAMPLE_DEMONSTRATES = "MARKER_VOICE_SAMPLE_DEMONSTRATES"
        const val MARKER_EXCLUDED_VERBAL_TIC = "MARKER_EXCLUDED_VERBAL_TIC"

        val EXCLUDED_MARKERS = listOf(
            MARKER_ONE_LINE,
            MARKER_LIE_STATEMENT,
            MARKER_SHARED_ROOT,
            MARKER_NEED,
            MARKER_WANT_NEED_CONFLICT,
            MARKER_TRAIT_REFLECTION,
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
