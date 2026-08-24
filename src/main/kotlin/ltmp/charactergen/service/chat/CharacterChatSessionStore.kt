package ltmp.charactergen.service.chat

import ltmp.charactergen.service.ChatSessionCapacityExceededException
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 캐릭터 대화 세션을 메모리에 보관한다.
 *
 * 마지막 접근으로부터 TTL이 지난 세션은 삭제한다. 주기적으로 쓸어내되,
 * 조회 시점에도 만료를 확인해서 아직 쓸리지 않은 세션이 살아 있는 것처럼 보이지 않게 한다.
 */
@Component
class CharacterChatSessionStore(
    @Value("\${character-chat.session.ttl}") private val ttl: Duration,
    @Value("\${character-chat.session.max-count}") private val maxCount: Int,
) {
    private val sessions = ConcurrentHashMap<String, CharacterChatSession>()

    init {
        require(maxCount > 0) { "character-chat.session.max-count must be greater than zero" }
        require(!ttl.isZero && !ttl.isNegative) { "character-chat.session.ttl must be positive" }
    }

    fun ttl(): Duration = ttl

    fun put(session: CharacterChatSession) {
        evictExpired()
        if (sessions.size >= maxCount) {
            throw ChatSessionCapacityExceededException("동시에 유지할 수 있는 대화 세션 수를 초과했습니다")
        }
        sessions[session.id] = session
    }

    fun find(sessionId: String): CharacterChatSession? {
        val session = sessions[sessionId] ?: return null
        if (session.isExpired(Instant.now(), ttl.seconds)) {
            sessions.remove(sessionId, session)
            return null
        }
        return session
    }

    fun size(): Int = sessions.size

    /** 보관 중인 세션을 모두 버린다. 테스트가 세션 상태를 초기화할 때 쓴다. */
    fun clear() {
        sessions.clear()
    }

    @Scheduled(fixedDelayString = "\${character-chat.session.sweep-interval-millis}")
    fun evictExpired() {
        val now = Instant.now()
        sessions.values.removeIf { it.isExpired(now, ttl.seconds) }
    }
}
