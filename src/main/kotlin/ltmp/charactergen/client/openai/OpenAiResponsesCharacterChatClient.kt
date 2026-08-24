package ltmp.charactergen.client.openai

import com.openai.client.OpenAIClient
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseInputItem
import ltmp.charactergen.service.chat.ChatRole
import org.springframework.stereotype.Component

/**
 * OpenAI Responses API의 스트리밍 호출로 캐릭터 응답을 받는다.
 *
 * Responses API는 상태를 갖지 않으므로 매 턴 인스트럭션과 전체 히스토리를 다시 보낸다.
 * 인스트럭션이 매 턴 재주입되는 셈이라, 턴이 쌓여도 캐릭터 지시가 희석되지 않는다.
 */
@Component
class OpenAiResponsesCharacterChatClient(
    private val openAIClient: OpenAIClient,
) : CharacterChatLlmClient {
    override fun streamReply(request: CharacterChatStreamRequest, onDelta: (String) -> Unit) {
        openAIClient.responses().createStreaming(createParams(request)).use { stream ->
            stream.stream().forEach { event ->
                event.error().ifPresent { error ->
                    throw IllegalStateException("OpenAI stream returned an error: ${error.message()}")
                }
                event.failed().ifPresent {
                    throw IllegalStateException("OpenAI stream failed before completion")
                }
                // 초기 버전에서는 실제 응답 텍스트 delta만 전달하고 나머지 이벤트는 넘긴다.
                event.outputTextDelta().ifPresent { delta -> onDelta(delta.delta()) }
            }
        }
    }

    internal fun createParams(request: CharacterChatStreamRequest): ResponseCreateParams {
        val inputItems = request.messages.map { message ->
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(
                        when (message.role) {
                            ChatRole.USER -> EasyInputMessage.Role.USER
                            ChatRole.ASSISTANT -> EasyInputMessage.Role.ASSISTANT
                        },
                    )
                    .content(message.content)
                    .build(),
            )
        }
        return ResponseCreateParams.builder()
            .instructions(request.instructions)
            .input(ResponseCreateParams.Input.ofResponse(inputItems))
            .model(request.model)
            .build()
    }
}
