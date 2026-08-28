package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.data.DisclosureLevel
import ltmp.charactergen.model.DisclosureLadder
import ltmp.charactergen.model.L0CharacterResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.sqrt

/**
 * 캐릭터 저장 시점에 판정한 런타임 주입 여부. 인덱스는 각 원본 배열 기준이다.
 *
 * `disclosureItemIndexes`만 원본 배열이 아니라 [runtimeOrderedItems]가 만든 평탄화 순서를 기준으로 한다.
 * 엔티티도 같은 순서로 만들어지므로 조립기와 매퍼가 같은 인덱스를 본다.
 */
data class RuntimeInclusion(
    val concreteDetailIndexes: Set<Int>,
    val verbalTicIndexes: Set<Int>,
    val disclosureItemIndexes: Set<Int>,
    val sentencePatternIncluded: Boolean,
)

/**
 * 개방 사다리를 런타임 판정과 엔티티 저장이 공유하는 하나의 순서로 편다.
 */
internal fun DisclosureLadder.runtimeOrderedItems(): List<Pair<DisclosureLevel, String>> =
    surface.map { DisclosureLevel.SURFACE to it } +
        mid.map { DisclosureLevel.MID to it } +
        deep.map { DisclosureLevel.DEEP to it } +
        deepest.map { DisclosureLevel.DEEPEST to it }

/**
 * 런타임 대화 프롬프트에서 서로 겹치는 [대화] 필드를 걸러낸다. (L0 캐릭터 생성 프롬프트 v4.2 B-13)
 *
 * v4.1에서는 `concrete_details`만 검사했는데, v4 결과물에서는 하나의 행동 축이 네다섯 개 필드에 나뉘어
 * 서술되는 일이 잦았다. 같은 말이 여러 번 들어가면 그 행동만 과도하게 강조되고 나머지 특징은 묻힌다.
 * v4.2는 생성 지시문(B-2~B-4, B-6, B-8)으로 이것을 먼저 막고, 이 판정을 백스톱으로 둔다.
 *
 * 문서의 우선순위 목록에서 상위 다섯 묶음(behavioral_rule, contradiction, relationship_events,
 * relationship_stages, behavior의 두 note)과 `disclosure_ladder.surface`는 제거하지 않고 비교 기준으로만 쓴다.
 * 이들은 캐릭터의 규칙 자체이거나 관계가 얕은 동안의 유일한 대화 소재라, 겹친다는 이유로 빼면
 * 프롬프트에서 규칙이 통째로 사라진다. 제거 대상은 그 아래 네 묶음이다.
 *
 * 비교 대상이 전부 L0의 불변값이라 판정 결과도 바뀌지 않으므로 캐릭터 저장 시점에 한 번만 계산하고,
 * 결과는 각 엔티티의 `runtimeIncluded`에 남긴다. 조립기는 그 값을 필터로만 쓴다.
 * 관계 단계별로 실제 주입되는 것은 한 단계뿐이지만 판정에는 세 단계를 모두 넣는다.
 * 단계마다 다른 결과를 저장하면 사용자마다 다른 프롬프트가 되고, 세 단계는 어차피 서로 달라야 하는 값이다.
 */
@Component
class RuntimeDuplicateSelector(
    private val embeddingClient: TextEmbeddingClient,
    @Value("\${character-runtime.duplicate.similarity-threshold}") private val similarityThreshold: Double,
    @Value("\${character-runtime.concrete-detail.max-count}") private val concreteDetailMaxCount: Int,
) {
    fun select(character: L0CharacterResponse): RuntimeInclusion {
        val keptTexts = baselineTexts(character).toMutableList()
        val candidates = candidates(character)

        val vectors = embed(keptTexts + candidates.map { it.text })
        val keptVectors = vectors.take(keptTexts.size).toMutableList()
        val candidateVectors = vectors.drop(keptTexts.size)

        val concreteDetailIndexes = mutableSetOf<Int>()
        val verbalTicIndexes = mutableSetOf<Int>()
        val disclosureItemIndexes = character.disclosureLadder.surface.indices.toMutableSet()
        var sentencePatternIncluded = false
        // 겹침 판정에서 살아남은 항목은 다음 항목의 비교 기준이 된다. 우선순위가 낮은 쪽이 빠지도록 순서대로 본다.
        val droppedDetailSimilarities = mutableMapOf<Int, Double>()

        candidates.forEachIndexed { position, candidate ->
            val vector = candidateVectors[position]
            val similarity = keptVectors.maxOfOrNull { cosineSimilarity(vector, it) } ?: 0.0
            if (similarity >= similarityThreshold) {
                if (candidate is RuntimeCandidate.ConcreteDetail) droppedDetailSimilarities[candidate.index] = similarity
                return@forEachIndexed
            }
            keptVectors.add(vector)
            when (candidate) {
                is RuntimeCandidate.VerbalTic -> verbalTicIndexes.add(candidate.index)
                is RuntimeCandidate.DisclosureItem -> disclosureItemIndexes.add(candidate.index)
                is RuntimeCandidate.SentencePattern -> sentencePatternIncluded = true
                is RuntimeCandidate.ConcreteDetail -> concreteDetailIndexes.add(candidate.index)
            }
        }

        return RuntimeInclusion(
            concreteDetailIndexes = cappedConcreteDetails(character, concreteDetailIndexes, droppedDetailSimilarities),
            verbalTicIndexes = verbalTicIndexes,
            disclosureItemIndexes = disclosureItemIndexes,
            sentencePatternIncluded = sentencePatternIncluded,
        )
    }

    /**
     * 제거하지 않는 필드. 우선순위가 가장 높은 묶음이라 비교 기준으로만 쓴다.
     */
    private fun baselineTexts(character: L0CharacterResponse): List<String> = buildList {
        addAll(character.core.lie.behavioralRule)
        add(character.core.contradiction.traitA)
        add(character.core.contradiction.traitB)
        with(character.relationshipEvents) {
            addAll(listOf(onRejection, onConflict, onAbsence, onOverApproach))
        }
        with(character.relationshipStages) {
            listOf(early, deepening, close).forEach {
                addAll(listOf(it.attachment, it.address, it.emotionalRange, it.avoidance))
            }
        }
        add(character.behavior.emotionalExpressionNote)
        add(character.behavior.avoidanceNote)
        addAll(character.disclosureLadder.surface)
    }

    /**
     * 제거 대상. 문서의 우선순위 순서대로 늘어놓는다.
     */
    private fun candidates(character: L0CharacterResponse): List<RuntimeCandidate> = buildList {
        character.voice.verbalTics.forEachIndexed { index, tic ->
            add(RuntimeCandidate.VerbalTic(index, tic.description))
        }
        character.disclosureLadder.runtimeOrderedItems().forEachIndexed { index, (level, text) ->
            if (level != DisclosureLevel.SURFACE) add(RuntimeCandidate.DisclosureItem(index, text))
        }
        add(RuntimeCandidate.SentencePattern(character.voice.sentencePattern))
        character.identity.concreteDetails.forEachIndexed { index, detail ->
            add(RuntimeCandidate.ConcreteDetail(index, detail))
        }
    }

    /**
     * 신변 디테일은 겹치지 않아도 전부 넣지 않는다. 비트 생성기용이 주 용도라 런타임에는 몇 개만 넣는다.
     * 전부 겹친다고 판정돼도 런타임에 하나도 남지 않게 두지는 않고, 가장 덜 겹치는 것부터 하한만큼 되살린다.
     */
    private fun cappedConcreteDetails(
        character: L0CharacterResponse,
        kept: Set<Int>,
        droppedSimilarities: Map<Int, Double>,
    ): Set<Int> {
        val total = character.identity.concreteDetails.size
        if (kept.isNotEmpty()) return kept.sorted().take(concreteDetailMaxCount).toSet()
        val restored = droppedSimilarities.entries
            .sortedBy { it.value }
            .take(minOf(MIN_CONCRETE_DETAIL_COUNT, total))
            .map { it.key }
        return restored.toSortedSet()
    }

    private fun embed(texts: List<String>): List<FloatArray> {
        val vectors = try {
            embeddingClient.embed(texts)
        } catch (failure: Exception) {
            throw IllegalStateException(
                "런타임에 넣을 [대화] 필드를 고르기 위한 임베딩 호출에 실패했습니다",
                failure,
            )
        }
        check(vectors.size == texts.size) {
            "임베딩 벡터 개수가 입력과 다릅니다: 입력 ${texts.size}개, 벡터 ${vectors.size}개"
        }
        return vectors
    }

    private fun cosineSimilarity(left: FloatArray, right: FloatArray): Double {
        check(left.size == right.size) { "임베딩 차원이 서로 다릅니다: ${left.size}, ${right.size}" }
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in left.indices) {
            val leftValue = left[index].toDouble()
            val rightValue = right[index].toDouble()
            dot += leftValue * rightValue
            leftNorm += leftValue * leftValue
            rightNorm += rightValue * rightValue
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0
        return dot / (sqrt(leftNorm) * sqrt(rightNorm))
    }

    private companion object {
        /** 신변 디테일이 전부 겹침으로 판정됐을 때도 남기는 최소 개수. */
        const val MIN_CONCRETE_DETAIL_COUNT = 2
    }
}

private sealed interface RuntimeCandidate {
    val text: String

    data class VerbalTic(val index: Int, override val text: String) : RuntimeCandidate

    data class DisclosureItem(val index: Int, override val text: String) : RuntimeCandidate

    data class SentencePattern(override val text: String) : RuntimeCandidate

    data class ConcreteDetail(val index: Int, override val text: String) : RuntimeCandidate
}
