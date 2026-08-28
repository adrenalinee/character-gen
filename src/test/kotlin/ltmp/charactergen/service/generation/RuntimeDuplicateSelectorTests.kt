package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.model.BannedWords
import ltmp.charactergen.model.CharacterBehavior
import ltmp.charactergen.model.CharacterContradiction
import ltmp.charactergen.model.CharacterCore
import ltmp.charactergen.model.CharacterIdentity
import ltmp.charactergen.model.CharacterLie
import ltmp.charactergen.model.CharacterRelation
import ltmp.charactergen.model.CharacterVoice
import ltmp.charactergen.model.DesignNote
import ltmp.charactergen.model.DisclosureLadder
import ltmp.charactergen.model.ForbiddenBehavior
import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.model.NonNegotiable
import ltmp.charactergen.model.NonNegotiableDomain
import ltmp.charactergen.model.RelationPresence
import ltmp.charactergen.model.RelationSlot
import ltmp.charactergen.model.RelationshipEvents
import ltmp.charactergen.model.RelationshipStageNote
import ltmp.charactergen.model.RelationshipStages
import ltmp.charactergen.model.VerbalTic
import ltmp.charactergen.model.VoiceSample
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 런타임에 넣을 [대화] 필드 선택 규칙. (L0 캐릭터 생성 프롬프트 v4.2 B-13)
 *
 * 벡터를 직접 쥐고 우선순위·임계값·상한·하한이 각각 어디서 걸리는지를 확인한다.
 * 벡터를 지정하지 않은 문장은 영벡터가 되어 무엇과도 겹치지 않는다.
 */
class RuntimeDuplicateSelectorTests {
    @Test
    fun `상위 필드와 유사도가 임계값을 넘는 신변 디테일은 제외한다`() {
        val selector = selector(
            "묘사1" to axis(0),
            "묘사2" to axis(1),
            "묘사3" to axis(2),
            "평소 행동 규칙" to axis(1),
        )

        val inclusion = selector.select(character(concreteDetails = listOf("묘사1", "묘사2", "묘사3")))

        assertEquals(setOf(0, 2), inclusion.concreteDetailIndexes)
    }

    @Test
    fun `겹치지 않는 신변 디테일이 많아도 상한까지만 남긴다`() {
        val selector = selector()

        val inclusion = selector.select(
            character(concreteDetails = listOf("묘사1", "묘사2", "묘사3", "묘사4", "묘사5")),
        )

        assertEquals(
            setOf(0, 1, 2),
            inclusion.concreteDetailIndexes,
            "상한을 넘으면 배열 앞쪽 세 개만 남아야 한다",
        )
    }

    @Test
    fun `신변 디테일이 전부 겹친다고 판정돼도 가장 덜 겹치는 두 개는 남긴다`() {
        // 세 묘사 모두 평소 행동 규칙과 임계값 이상으로 겹치되, 겹치는 정도는 서로 다르게 둔다.
        val selector = selector(
            "묘사1" to vector(0.90f, 0.44f, 0f),
            "묘사2" to vector(1f, 0f, 0f),
            "묘사3" to vector(0.95f, 0.31f, 0f),
            "평소 행동 규칙" to axis(0),
        )

        val inclusion = selector.select(character(concreteDetails = listOf("묘사1", "묘사2", "묘사3")))

        assertEquals(
            setOf(0, 2),
            inclusion.concreteDetailIndexes,
            "런타임에 신변 디테일이 하나도 남지 않으면 안 된다",
        )
    }

    @Test
    fun `상위 필드와 겹치는 말버릇과 문장 패턴은 제외한다`() {
        val selector = selector(
            "말버릇2" to axis(0),
            "정서 표출 서술" to axis(0),
            "문장 패턴" to axis(1),
            "거절당했을 때" to axis(1),
        )

        val inclusion = selector.select(character())

        assertEquals(setOf(0, 2, 3), inclusion.verbalTicIndexes)
        assertFalse(inclusion.sentencePatternIncluded)
    }

    @Test
    fun `상위 필드와 겹치는 개방 항목은 제외하고 surface는 남긴다`() {
        val selector = selector(
            "surface1" to axis(0),
            "회피 서술" to axis(0),
            "mid1" to axis(1),
            "관계 단계 애착" to axis(1),
        )

        val inclusion = selector.select(character())

        assertTrue(
            0 in inclusion.disclosureItemIndexes,
            "surface는 관계가 얕은 동안의 유일한 대화 소재라 겹쳐도 빼지 않는다",
        )
        assertFalse(2 in inclusion.disclosureItemIndexes, "겹치는 mid 항목은 빠져야 한다")
        assertTrue(3 in inclusion.disclosureItemIndexes)
    }

    @Test
    fun `겹치는 것이 없으면 모두 남긴다`() {
        val inclusion = selector().select(character())

        assertEquals(setOf(0, 1, 2, 3), inclusion.verbalTicIndexes)
        assertTrue(inclusion.sentencePatternIncluded)
        assertEquals((0..4).toSet(), inclusion.disclosureItemIndexes)
    }

    private fun selector(vararg vectors: Pair<String, FloatArray>): RuntimeDuplicateSelector {
        val byText = vectors.toMap()
        val client = object : TextEmbeddingClient {
            // 벡터를 지정하지 않은 문장은 영벡터라 코사인 유사도가 언제나 0이다.
            override fun embed(texts: List<String>): List<FloatArray> =
                texts.map { byText[it] ?: FloatArray(DIMENSION) }
        }
        return RuntimeDuplicateSelector(client, SIMILARITY_THRESHOLD, MAX_CONCRETE_DETAIL_COUNT)
    }

    /** 지정한 축만 1인 단위 벡터. 서로 다른 축끼리는 코사인 0이다. */
    private fun axis(index: Int): FloatArray = FloatArray(DIMENSION).also { it[index] = 1f }

    private fun vector(vararg values: Float): FloatArray =
        FloatArray(DIMENSION).also { values.forEachIndexed { index, value -> it[index] = value } }

    private fun character(
        concreteDetails: List<String> = listOf("묘사1", "묘사2", "묘사3"),
    ): L0CharacterResponse = L0CharacterResponse(
        identity = CharacterIdentity(
            name = "서윤",
            age = 29,
            gender = "여성",
            occupation = "웹툰 작가",
            dailyLife = "새벽에 작업하고 낮에 잔다",
            oneLine = "한 줄 요약",
            concreteDetails = concreteDetails,
        ),
        core = CharacterCore(
            lie = CharacterLie(
                type = "대체 가능성 공포형",
                statement = "믿음",
                behavioralRule = listOf("평소 행동 규칙"),
                wound = "상처",
            ),
            want = "쫓는 것",
            need = "필요한 것",
            wantNeedConflict = "충돌",
            contradiction = CharacterContradiction("특성 A", "특성 B", "공통 뿌리"),
            nonNegotiables = listOf(
                NonNegotiable(NonNegotiableDomain.`관계를 다루는 방식`, "굽히지 않는 것", "연결", true),
            ),
            forbiddenBehaviors = listOf(ForbiddenBehavior("하지 않는 것", "연결")),
        ),
        relationshipStages = RelationshipStages(
            early = stageNote("관계 단계 애착"),
            deepening = stageNote("깊어질 때 애착"),
            close = stageNote("가까울 때 애착"),
        ),
        relationshipEvents = RelationshipEvents(
            onRejection = "거절당했을 때",
            onConflict = "부딪혔을 때",
            onAbsence = "오랜만일 때",
            onOverApproach = "너무 다가올 때",
        ),
        behavior = CharacterBehavior(
            emotionalExpressionNote = "정서 표출 서술",
            avoidanceNote = "회피 서술",
        ),
        voice = CharacterVoice(
            speechArchetypeNote = "말투 원형 설명",
            sentencePattern = "문장 패턴",
            verbalTics = (1..4).map { VerbalTic("상황$it", "말버릇$it") },
            bannedWords = BannedWords(fromLie = listOf("무조건"), fromBackground = listOf("당연히")),
        ),
        relations = listOf(
            CharacterRelation(RelationSlot.과거, "이름", "관계", "설명", RelationPresence.부재, "연결"),
        ),
        disclosureLadder = DisclosureLadder(
            surface = listOf("surface1", "surface2"),
            mid = listOf("mid1", "mid2"),
            deep = listOf("deep1"),
            deepest = emptyList(),
        ),
        voiceSamples = listOf(VoiceSample("상황", "발화", "무엇을 보여주는가")),
        designNote = DesignNote("판별", "장기", "없음", "성향 반영"),
    )

    private fun stageNote(attachment: String) = RelationshipStageNote(
        attachment = attachment,
        address = "$attachment 호칭",
        emotionalRange = "$attachment 감정",
        avoidance = "$attachment 회피",
    )

    private companion object {
        const val DIMENSION = 8
        const val SIMILARITY_THRESHOLD = 0.75
        const val MAX_CONCRETE_DETAIL_COUNT = 3
    }
}
