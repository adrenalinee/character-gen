package ltmp.charactergen.service.generation

import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.model.L0CharacterResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.sqrt

/**
 * 런타임 대화 프롬프트에 넣을 `concrete_details`를 고른다. (L0 캐릭터 생성 프롬프트 v4.1 B-10)
 *
 * v4 결과물에서 `concrete_details`가 `disclosure_ladder.surface`, `behavior.emotional_expression_note`,
 * `core.lie.behavioral_rule`과 사실상 같은 내용을 두 번 말하는 사례가 나왔다. 같은 말이 두 번 들어가면
 * 프롬프트만 길어지고 규칙은 흐려진다.
 *
 * 비교 대상이 전부 L0의 불변값이라 판정 결과도 바뀌지 않으므로 캐릭터 저장 시점에 한 번만 계산하고,
 * 결과는 `CharacterConcreteDetailEntity.runtimeIncluded`에 남긴다. 조립기는 그 값을 필터로만 쓴다.
 */
@Component
class RuntimeConcreteDetailSelector(
    private val embeddingClient: TextEmbeddingClient,
    @Value("\${character-runtime.concrete-detail.similarity-threshold}") private val similarityThreshold: Double,
    @Value("\${character-runtime.concrete-detail.max-count}") private val maxCount: Int,
) {
    /**
     * 런타임에 넣을 항목의 인덱스를 [L0CharacterResponse.identity]의 `concrete_details` 배열 기준으로 돌려준다.
     */
    fun select(character: L0CharacterResponse): Set<Int> {
        val details = character.identity.concreteDetails
        // 하한 이하로는 어떻게 판정해도 전부 남으므로 임베딩을 부르지 않는다.
        if (details.size <= MIN_COUNT) return details.indices.toSet()

        val comparisons = buildList {
            addAll(character.disclosureLadder.surface)
            add(character.behavior.emotionalExpressionNote)
            addAll(character.core.lie.behavioralRule)
        }
        if (comparisons.isEmpty()) return details.indices.take(maxCount).toSet()

        val vectors = try {
            embeddingClient.embed(details + comparisons)
        } catch (failure: Exception) {
            throw IllegalStateException(
                "런타임에 넣을 concrete_details를 고르기 위한 임베딩 호출에 실패했습니다",
                failure,
            )
        }
        check(vectors.size == details.size + comparisons.size) {
            "임베딩 벡터 개수가 입력과 다릅니다: 입력 ${details.size + comparisons.size}개, 벡터 ${vectors.size}개"
        }
        val comparisonVectors = vectors.drop(details.size)
        val maxSimilarities = vectors.take(details.size).map { detailVector ->
            comparisonVectors.maxOf { cosineSimilarity(detailVector, it) }
        }

        val kept = details.indices.filter { maxSimilarities[it] < similarityThreshold }
        if (kept.isNotEmpty()) return kept.take(maxCount).toSet()

        // 전부 겹친다고 판정돼도 런타임에 신변 디테일이 하나도 남지 않게 두지는 않는다.
        // 가장 덜 겹치는 것부터 하한만큼 남기고, 순서는 원래 배열 순서를 지킨다.
        return details.indices
            .sortedBy { maxSimilarities[it] }
            .take(minOf(MIN_COUNT, details.size))
            .toSortedSet()
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
        /** 겹침 판정이 전부 걸렸을 때도 남기는 최소 개수. */
        const val MIN_COUNT = 2
    }
}
