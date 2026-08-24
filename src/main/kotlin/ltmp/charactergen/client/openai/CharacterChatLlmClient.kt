package ltmp.charactergen.client.openai

import ltmp.charactergen.service.chat.CharacterChatMessage

data class CharacterChatStreamRequest(
    val model: String,
    val instructions: String,
    val messages: List<CharacterChatMessage>,
)

/**
 * 캐릭터 대화 응답을 조각 단위로 흘려보내는 LLM 클라이언트.
 *
 * 구현은 텍스트 delta만 [onDelta]로 넘기고 나머지 이벤트는 버린다.
 * 호출은 블로킹이며, 스트림이 끝나면 반환한다.
 */
interface CharacterChatLlmClient {
    fun streamReply(request: CharacterChatStreamRequest, onDelta: (String) -> Unit)
}
