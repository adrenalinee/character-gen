package ltmp.charactergen.service.chat

import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.RelationshipStage
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class CharacterChatMessage(
    val role: ChatRole,
    val content: String,
    val createdAt: OffsetDateTime,
)

/**
 * 메모리에만 존재하는 캐릭터 대화 세션.
 *
 * 인스트럭션은 세션 생성 시점에 조립을 끝내고 문자열로 들고 있는다.
 * 이후 스트리밍 스레드는 DB를 건드리지 않는다.
 */
class CharacterChatSession(
    val id: String,
    val characterId: Long,
    val characterName: String,
    val relationshipStage: RelationshipStage,
    val instructionBranch: String,
    val instructions: String,
    val model: String,
    val situationNote: String?,
    val includeVoiceSamples: Boolean,
    val closedTopicMode: ClosedTopicMode,
    val createdAt: OffsetDateTime,
) {
    /**
     * 한 세션에서 동시에 두 턴이 진행되지 않게 막는다.
     *
     * 턴 시작은 서블릿 스레드에서, 종료는 스트리밍 스레드에서 일어나므로
     * 잠근 스레드에서만 풀 수 있는 Lock 대신 플래그를 쓴다.
     */
    private val turnInProgress = AtomicBoolean(false)

    private val messages = mutableListOf<CharacterChatMessage>()

    // 만료 판정 기준값이라 생성 시각으로 초기화한다.
    @Volatile
    var lastAccessedAt: Instant = Instant.now()
        private set

    fun touch() {
        lastAccessedAt = Instant.now()
    }

    fun isExpired(now: Instant, ttlSeconds: Long): Boolean =
        lastAccessedAt.plusSeconds(ttlSeconds).isBefore(now)

    fun tryBeginTurn(): Boolean = turnInProgress.compareAndSet(false, true)

    fun endTurn() {
        turnInProgress.set(false)
    }

    fun messages(): List<CharacterChatMessage> = synchronized(messages) { messages.toList() }

    /**
     * 사용자 메시지와 응답을 함께 기록한다.
     *
     * 스트림이 끝까지 성공한 뒤에만 호출한다. 중간에 실패했는데 사용자 메시지만 남으면
     * 응답이 없는 턴이 히스토리에 섞여 이후 턴을 오염시킨다.
     */
    fun appendTurn(userMessage: CharacterChatMessage, assistantMessage: CharacterChatMessage): Int =
        synchronized(messages) {
            messages.add(userMessage)
            messages.add(assistantMessage)
            messages.lastIndex
        }
}
