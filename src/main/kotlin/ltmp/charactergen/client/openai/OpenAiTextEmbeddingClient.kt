package ltmp.charactergen.client.openai

import com.openai.client.OpenAIClient
import com.openai.models.embeddings.EmbeddingCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OpenAiTextEmbeddingClient(
    @Value("\${openai.embedding.model}") private val model: String,
    private val openAIClient: OpenAIClient,
) : TextEmbeddingClient {
    /**
     * 응답의 `index`는 입력 순서를 가리키지만 순서대로 온다는 보장이 없으므로 명시적으로 되돌린다.
     */
    override fun embed(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()
        val params = EmbeddingCreateParams.builder()
            .model(model)
            .inputOfArrayOfStrings(texts)
            .build()
        val data = openAIClient.embeddings().create(params).data()
        check(data.size == texts.size) {
            "임베딩 응답 개수가 입력과 다릅니다: 입력 ${texts.size}개, 응답 ${data.size}개"
        }
        val byIndex = arrayOfNulls<FloatArray>(texts.size)
        for (embedding in data) {
            val index = embedding.index().toInt()
            check(index in texts.indices) { "임베딩 응답 index가 범위를 벗어났습니다: $index" }
            byIndex[index] = embedding.embedding().toFloatArray()
        }
        return byIndex.mapIndexed { index, vector ->
            checkNotNull(vector) { "임베딩 응답에 index=$index 항목이 없습니다" }
        }
    }
}
