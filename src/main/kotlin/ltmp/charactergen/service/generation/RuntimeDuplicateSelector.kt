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
 *
 * [warnings]는 하한 때문에 겹치는데도 제거하지 못한 항목이다. 값은 그대로 두고 기록만 남긴다.
 * 반복 생성에서 같은 경고가 쌓이면 생성 프롬프트를 고칠 근거가 된다.
 */
data class RuntimeInclusion(
    val concreteDetailIndexes: Set<Int>,
    val verbalTicIndexes: Set<Int>,
    val disclosureItemIndexes: Set<Int>,
    val sentencePatternIncluded: Boolean,
    val warnings: List<String>,
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
 * 런타임 대화 프롬프트에서 서로 겹치는 [대화] 필드를 걸러낸다.
 * (런타임 프롬프트 조립 스펙 v1 4-1, L0 캐릭터 생성 프롬프트 v4.2 B-13)
 *
 * v4.1에서는 `concrete_details`만 검사했는데, v4 결과물에서는 하나의 행동 축이 네다섯 개 필드에 나뉘어
 * 서술되는 일이 잦았다. 같은 말이 여러 번 들어가면 그 행동만 과도하게 강조되고 나머지 특징은 묻힌다.
 * v4.2는 생성 지시문(B-2~B-4, B-6, B-8)으로 이것을 먼저 막고, 이 판정을 백스톱으로 둔다.
 *
 * **스펙 v1에서 제거 대상과 비교 기준이 뒤집혔다.** `disclosure_ladder.surface`가 제거 대상으로,
 * `voice.sentence_pattern`과 surface가 아닌 개방 항목이 비교 기준으로 옮겨졌다.
 * surface를 절대 빼지 않던 근거(관계가 얕은 동안의 유일한 대화 소재)는 하한 [CandidateGroup.SURFACE]의 3개가 대신한다.
 * surface는 스키마상 3~5개이므로 하한 3이면 실제로 최대 두 개만 빠진다.
 *
 * 스펙의 비교 기준에는 "개방된 disclosure 항목"이라고 적혀 있으나 어느 층위가 열릴지는 런타임에 정해진다.
 * 생성 시점에는 알 수 없으므로 surface가 아닌 항목 전체를 기준으로 두는 보수적 해석을 택했다.
 *
 * 개수 자체가 캐릭터 정의의 일부인 배열(`non_negotiables`, `forbidden_behaviors`, `lie.behavioral_rule`,
 * `relationship_events`, `relationship_stages`)은 후보에도 기준에도 넣지 않아 애초에 건드리지 않는다.
 * 이 배열들의 개수 미달은 [L0CharacterCountValidator]가 따로 본다.
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
        val keptTexts = baselineTexts(character)
        val candidates = candidates(character)

        val vectors = embed(keptTexts + candidates.map { it.text })
        val keptVectors = vectors.take(keptTexts.size).toMutableList()
        val candidateVectors = vectors.drop(keptTexts.size)

        val concreteDetailIndexes = mutableSetOf<Int>()
        val verbalTicIndexes = mutableSetOf<Int>()
        val disclosureItemIndexes = nonSurfaceDisclosureIndexes(character).toMutableSet()
        val warnings = mutableListOf<String>()
        val budget = DropBudget(character)

        // 겹침 판정에서 살아남은 항목은 다음 항목의 비교 기준이 된다. 우선순위가 낮은 쪽이 빠지도록 순서대로 본다.
        candidates.forEachIndexed { position, candidate ->
            val vector = candidateVectors[position]
            val similarity = keptVectors.maxOfOrNull { cosineSimilarity(vector, it) } ?: 0.0
            if (similarity >= similarityThreshold) {
                if (budget.tryDrop(candidate.group)) return@forEachIndexed
                // 하한에 도달했으면 겹쳐도 빼지 않는다. 규칙이 통째로 사라지는 것보다 겹치는 편이 낫다.
                warnings.add(duplicateKeptWarning(candidate, similarity))
            }
            keptVectors.add(vector)
            when (candidate) {
                is RuntimeCandidate.VerbalTic -> verbalTicIndexes.add(candidate.index)
                is RuntimeCandidate.SurfaceDisclosure -> disclosureItemIndexes.add(candidate.index)
                is RuntimeCandidate.ConcreteDetail -> concreteDetailIndexes.add(candidate.index)
            }
        }

        return RuntimeInclusion(
            concreteDetailIndexes = concreteDetailIndexes.sorted().take(concreteDetailMaxCount).toSet(),
            verbalTicIndexes = verbalTicIndexes,
            disclosureItemIndexes = disclosureItemIndexes,
            // 문장 패턴은 스펙 4-1에서 비교 기준으로 옮겨져 겹침 판정으로는 더 이상 빠지지 않는다.
            // 운영자가 손으로 끌 여지를 남기기 위해 필드와 조립기 분기는 유지한다.
            sentencePatternIncluded = true,
            warnings = warnings,
        )
    }

    /**
     * 제거하지 않는 필드. 비교 기준으로만 쓴다. (스펙 4-1의 비교 B에서 A를 뺀 것)
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
        add(character.voice.sentencePattern)
        character.disclosureLadder.runtimeOrderedItems()
            .filter { (level, _) -> level != DisclosureLevel.SURFACE }
            .forEach { (_, text) -> add(text) }
    }

    /**
     * 제거 대상. 스펙 4-1의 A 내부 우선순위(verbal_tics > surface > concrete_details) 순서대로 늘어놓는다.
     */
    private fun candidates(character: L0CharacterResponse): List<RuntimeCandidate> = buildList {
        character.voice.verbalTics.forEachIndexed { index, tic ->
            add(RuntimeCandidate.VerbalTic(index, tic.description))
        }
        character.disclosureLadder.surface.forEachIndexed { index, text ->
            // 평탄화 순서에서 surface가 맨 앞이라 원본 인덱스가 그대로 평탄화 인덱스다.
            add(RuntimeCandidate.SurfaceDisclosure(index, text))
        }
        character.identity.concreteDetails.forEachIndexed { index, detail ->
            add(RuntimeCandidate.ConcreteDetail(index, detail))
        }
    }

    private fun nonSurfaceDisclosureIndexes(character: L0CharacterResponse): Set<Int> =
        character.disclosureLadder.runtimeOrderedItems()
            .withIndex()
            .filter { (_, item) -> item.first != DisclosureLevel.SURFACE }
            .mapTo(mutableSetOf()) { it.index }

    private fun duplicateKeptWarning(candidate: RuntimeCandidate, similarity: Double): String =
        "${candidate.group.label}이(가) 다른 대화용 필드와 겹치지만 최소 ${candidate.group.floor}개를 지키느라 " +
            "제거하지 않았습니다(유사도 ${"%.2f".format(similarity)}): ${candidate.text}"

    /**
     * 그룹별로 몇 개까지 뺄 수 있는지 센다. 하한에 걸리면 [tryDrop]이 false를 돌려준다. (스펙 4-1의 최소 개수 하한)
     */
    private class DropBudget(character: L0CharacterResponse) {
        private val remaining = mutableMapOf<CandidateGroup, Int>().apply {
            put(CandidateGroup.VERBAL_TIC, droppable(character.voice.verbalTics.size, CandidateGroup.VERBAL_TIC))
            put(
                CandidateGroup.SURFACE,
                droppable(character.disclosureLadder.surface.size, CandidateGroup.SURFACE),
            )
            put(
                CandidateGroup.CONCRETE_DETAIL,
                droppable(character.identity.concreteDetails.size, CandidateGroup.CONCRETE_DETAIL),
            )
        }

        fun tryDrop(group: CandidateGroup): Boolean {
            val left = remaining.getValue(group)
            if (left <= 0) return false
            remaining[group] = left - 1
            return true
        }

        private fun droppable(total: Int, group: CandidateGroup): Int = maxOf(0, total - group.floor)
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
}

/** 제거 대상 묶음과 그 최소 개수 하한. (스펙 4-1) */
internal enum class CandidateGroup(val label: String, val floor: Int) {
    VERBAL_TIC("말버릇", 2),
    SURFACE("가벼운 개인사", 3),
    CONCRETE_DETAIL("신변 디테일", 2),
}

private sealed interface RuntimeCandidate {
    val text: String
    val group: CandidateGroup

    data class VerbalTic(val index: Int, override val text: String) : RuntimeCandidate {
        override val group = CandidateGroup.VERBAL_TIC
    }

    data class SurfaceDisclosure(val index: Int, override val text: String) : RuntimeCandidate {
        override val group = CandidateGroup.SURFACE
    }

    data class ConcreteDetail(val index: Int, override val text: String) : RuntimeCandidate {
        override val group = CandidateGroup.CONCRETE_DETAIL
    }
}
