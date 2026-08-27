package ltmp.charactergen.client.openai

/**
 * 테스트용 임베딩 대역.
 *
 * 배치 안에서 서로 다른 문자열에 서로 다른 축을 하나씩 배정한다. 그래서 문자열이 정확히 같으면 코사인 1,
 * 다르면 0이 된다. 의미 유사도는 재지 못하지만 완전히 겹치는 항목이 런타임에서 빠지는지는 결정적으로 확인할 수 있고,
 * 해시 충돌 같은 우연에 결과가 흔들리지 않는다.
 */
class FakeTextEmbeddingClient : TextEmbeddingClient {
    override fun embed(texts: List<String>): List<FloatArray> {
        val axes = texts.distinct()
        return texts.map { text ->
            FloatArray(axes.size).also { it[axes.indexOf(text)] = 1f }
        }
    }
}
