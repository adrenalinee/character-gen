package ltmp.charactergen.service.generation

import ltmp.charactergen.data.VerbalTicSituationGroup
import ltmp.charactergen.data.VoiceSampleSituationGroup
import ltmp.charactergen.data.entity.VerbalTicSituationEntity
import ltmp.charactergen.data.entity.VoiceSampleSituationEntity
import ltmp.charactergen.data.repo.VerbalTicSituationRepository
import ltmp.charactergen.data.repo.VoiceSampleSituationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 프롬프트에 넣을 상황을 카탈로그에서 고른다. (L0 캐릭터 생성 프롬프트 v4 §4)
 *
 * 어떤 상황을 주느냐가 캐릭터를 가르는 축을 결정하므로 모델에 맡기지 않고 서버가 군 단위로 고른다.
 * 고르는 규칙은 군마다 검증하는 축이 다르다는 것에서 나온다.
 */
@Service
class L0SituationSelectionService(
    private val verbalTicSituationRepository: VerbalTicSituationRepository,
    private val voiceSampleSituationRepository: VoiceSampleSituationRepository,
) {
    /**
     * 말버릇 상황 세 개. 네 번째는 모델이 스스로 정하므로 여기서 고르지 않는다. (§4.1 선택 규칙)
     *
     * D군과 C군이 캐릭터를 가장 크게 가르는 축(회피 방식·정서 표출)을 검증하므로 각각 한 개를 고정으로 넣고,
     * 나머지 한 개는 A군과 B군을 합친 데서 고른다.
     */
    @Transactional(readOnly = true)
    fun selectVerbalTicSituations(): List<VerbalTicSituationEntity> {
        val byGroup = verbalTicSituationRepository.findAllByOrderByIdAsc().groupBy { it.group }
        return listOf(
            pickOne(byGroup, listOf(VerbalTicSituationGroup.SELF_DOMAIN)),
            pickOne(byGroup, listOf(VerbalTicSituationGroup.EMOTIONAL_TRIGGER)),
            pickOne(
                byGroup,
                listOf(VerbalTicSituationGroup.CONVERSATION_FLOW, VerbalTicSituationGroup.REQUEST_RESPONSE),
            ),
        )
    }

    /**
     * 발화 샘플 상황 세 개. (§4.2 선택 규칙)
     *
     * 1군에서 기본 톤을 하나 잡고, 3군과 4군에서 한두 개를 고른 뒤 남은 자리를 2군으로 채운다.
     * 3군을 두 개 이상 뽑으면 캐릭터가 실제보다 완고해 보이므로 3군은 한 개까지만 허용한다.
     */
    @Transactional(readOnly = true)
    fun selectVoiceSampleSituations(): List<VoiceSampleSituationEntity> {
        val byGroup = voiceSampleSituationRepository.findAllByOrderByIdAsc().groupBy { it.group }
        val opening = pickOne(byGroup, listOf(VoiceSampleSituationGroup.RELATIONSHIP_START))
        val tension = pickTension(byGroup)
        val goodwill = pick(
            byGroup,
            listOf(VoiceSampleSituationGroup.GOODWILL),
            VOICE_SAMPLE_COUNT - 1 - tension.size,
        )
        return listOf(opening) + tension + goodwill
    }

    /**
     * 3군·4군에서 한두 개. 3군은 한 개를 넘지 않는다.
     *
     * 3군 제한 때문에 목표 개수를 못 채우면 그만큼 2군이 더 들어간다. 선택 규칙의 "나머지는 2군"이 그 자리를 받는다.
     */
    private fun pickTension(
        byGroup: Map<VoiceSampleSituationGroup, List<VoiceSampleSituationEntity>>,
    ): List<VoiceSampleSituationEntity> {
        val count = (1..MAX_TENSION_COUNT).random()
        val candidates = (
            groupOf(byGroup, VoiceSampleSituationGroup.FRICTION) +
                groupOf(byGroup, VoiceSampleSituationGroup.VULNERABILITY)
            ).shuffled()
        val picked = mutableListOf<VoiceSampleSituationEntity>()
        var frictionCount = 0
        for (candidate in candidates) {
            if (picked.size == count) break
            if (candidate.group == VoiceSampleSituationGroup.FRICTION) {
                if (frictionCount == MAX_FRICTION_COUNT) continue
                frictionCount++
            }
            picked += candidate
        }
        return picked
    }

    private fun <G, T> pickOne(byGroup: Map<G, List<T>>, groups: List<G>): T = pick(byGroup, groups, 1).single()

    /**
     * 주어진 군들을 합친 데서 [count]개를 무작위로 고른다. 후보가 모자라면 카탈로그가 잘못 채워진 것이므로 실패시킨다.
     */
    private fun <G, T> pick(byGroup: Map<G, List<T>>, groups: List<G>, count: Int): List<T> {
        val candidates = groups.flatMap { groupOf(byGroup, it) }
        check(candidates.size >= count) {
            "상황 카탈로그에 후보가 부족합니다: groups=$groups, required=$count, found=${candidates.size}"
        }
        return candidates.shuffled().take(count)
    }

    private fun <G, T> groupOf(byGroup: Map<G, List<T>>, group: G): List<T> = byGroup[group].orEmpty()

    private companion object {
        const val VOICE_SAMPLE_COUNT = 3

        /** 3군과 4군에서 최대 두 개까지 뽑는다. */
        const val MAX_TENSION_COUNT = 2

        /** 3군(마찰)은 한 개를 넘기지 않는다. */
        const val MAX_FRICTION_COUNT = 1
    }
}
