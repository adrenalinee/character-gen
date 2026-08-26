package ltmp.charactergen.service.generation

import ltmp.charactergen.data.VerbalTicSituationGroup
import ltmp.charactergen.data.VoiceSampleSituationGroup
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 상황 선택 규칙(v4 §4.1, §4.2)이 지켜지는지 검증한다.
 *
 * 선택에 무작위가 들어가므로 한 번의 결과가 아니라 여러 번 뽑아 규칙이 항상 성립하는지 본다.
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
class L0SituationSelectionIntegrationTests(
    private val situationSelectionService: L0SituationSelectionService,
) {
    @Test
    fun `말버릇 상황은 D군 하나 C군 하나 그리고 A군이나 B군 하나로 고른다`() {
        repeat(DRAW_COUNT) {
            val selected = situationSelectionService.selectVerbalTicSituations()

            assertEquals(3, selected.size)
            assertEquals(3, selected.map { it.situation }.toSet().size, "같은 상황이 두 번 들어갔다")
            val groups = selected.map { it.group }
            assertEquals(1, groups.count { it == VerbalTicSituationGroup.SELF_DOMAIN }, "회피 방식 검증군이 빠졌다")
            assertEquals(
                1,
                groups.count { it == VerbalTicSituationGroup.EMOTIONAL_TRIGGER },
                "정서 표출 검증군이 빠졌다",
            )
            assertEquals(
                1,
                groups.count {
                    it == VerbalTicSituationGroup.CONVERSATION_FLOW ||
                        it == VerbalTicSituationGroup.REQUEST_RESPONSE
                },
            )
        }
    }

    @Test
    fun `발화 샘플 상황은 1군 하나로 시작하고 마찰군을 두 개 이상 뽑지 않는다`() {
        repeat(DRAW_COUNT) {
            val selected = situationSelectionService.selectVoiceSampleSituations()

            assertEquals(3, selected.size)
            assertEquals(3, selected.map { it.situation }.toSet().size, "같은 상황이 두 번 들어갔다")
            val groups = selected.map { it.group }
            assertEquals(
                VoiceSampleSituationGroup.RELATIONSHIP_START,
                groups.first(),
                "기본 톤을 잡는 1군이 첫 자리에 없다",
            )
            assertEquals(1, groups.count { it == VoiceSampleSituationGroup.RELATIONSHIP_START })
            assertTrue(
                groups.count { it == VoiceSampleSituationGroup.FRICTION } <= 1,
                "3군을 두 개 이상 뽑으면 캐릭터가 실제보다 완고해 보인다",
            )
            assertTrue(
                groups.any {
                    it == VoiceSampleSituationGroup.FRICTION || it == VoiceSampleSituationGroup.VULNERABILITY
                },
                "3군이나 4군이 하나도 없으면 마찰과 취약을 확인할 수 없다",
            )
        }
    }

    private companion object {
        /** 무작위 선택이므로 규칙 위반이 드물게라도 나오는지 보려면 여러 번 뽑아야 한다. */
        const val DRAW_COUNT = 30
    }
}
