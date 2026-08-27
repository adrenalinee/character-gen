package ltmp.charactergen.client.openai

/**
 * 문장 사이의 의미 유사도를 재기 위한 임베딩 클라이언트.
 *
 * 캐릭터 저장 시점에 런타임 프롬프트에 넣을 `concrete_details`를 고르는 데 쓴다.
 * (L0 캐릭터 생성 프롬프트 v4.1 B-10)
 */
interface TextEmbeddingClient {
    /**
     * 입력 순서를 그대로 유지한 임베딩 벡터를 돌려준다.
     *
     * 호출 한 번으로 모든 문장을 처리하므로 벡터 차원은 서로 같다.
     */
    fun embed(texts: List<String>): List<FloatArray>
}
