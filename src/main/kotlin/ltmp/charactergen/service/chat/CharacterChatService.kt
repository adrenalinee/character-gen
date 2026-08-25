package ltmp.charactergen.service.chat

import jakarta.annotation.PreDestroy
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.api.chat.CharacterChatDeltaEventDto
import ltmp.charactergen.api.chat.CharacterChatDoneEventDto
import ltmp.charactergen.api.chat.CharacterChatMessageRequest
import ltmp.charactergen.api.chat.CharacterChatSessionDto
import ltmp.charactergen.api.chat.CreateCharacterChatSessionRequest
import ltmp.charactergen.api.chat.toDto
import ltmp.charactergen.client.openai.CharacterChatLlmClient
import ltmp.charactergen.client.openai.CharacterChatStreamRequest
import ltmp.charactergen.data.ClosedTopicMode
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.service.ChatSessionCapacityExceededException
import ltmp.charactergen.service.DefaultPromptTemplateService
import ltmp.charactergen.service.InvalidRequestException
import ltmp.charactergen.service.ResourceConflictException
import ltmp.charactergen.service.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@Service
class CharacterChatService(
    @Value("\${openai.chat.model}") private val defaultModel: String,
    @Value("\${character-chat.include-voice-samples}") private val defaultIncludeVoiceSamples: Boolean,
    @Value("\${character-chat.stream.timeout}") private val streamTimeout: Duration,
    @Value("\${character-chat.session.max-concurrency}") maxConcurrency: Int,
    private val characterRepository: CharacterRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val defaultPromptTemplateService: DefaultPromptTemplateService,
    private val instructionRenderer: CharacterRuntimeInstructionRenderer,
    private val sessionStore: CharacterChatSessionStore,
    private val chatLlmClient: CharacterChatLlmClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val permits: Semaphore
    private val executorService: ExecutorService

    private val datetimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)

    init {
        require(maxConcurrency > 0) { "character-chat.session.max-concurrency must be greater than zero" }
        permits = Semaphore(maxConcurrency, true)
        executorService = Executors.newFixedThreadPool(
            maxConcurrency,
            Thread.ofPlatform().name("character-chat-", 1).factory(),
        )
    }

    /**
     * 캐릭터를 런타임 인스트럭션으로 조립해 대화 세션을 만든다.
     *
     * 조립은 이 트랜잭션 안에서 끝내고 결과를 문자열로 보관한다.
     * 이후 스트리밍 스레드는 DB에 접근하지 않는다.
     */
    @Transactional(readOnly = true)
    fun createSession(request: CreateCharacterChatSessionRequest): CharacterChatSessionDto {
        val character = characterRepository.findOneWithGenerationById(request.characterId)
            ?: throw ResourceNotFoundException("Character not found: ${request.characterId}")

        val ref = resolveRuntimeRef(request.refId, request.instructionBranch)

        val options = RuntimeInstructionOptions(
            relationshipStage = request.relationshipStage,
            situationNote = request.situationNote?.takeIf { it.isNotBlank() },
            includeVoiceSamples = request.includeVoiceSamples ?: defaultIncludeVoiceSamples,
            closedTopicMode = request.closedTopicMode ?: ClosedTopicMode.TOPIC_ONLY,
        )
        val session = CharacterChatSession(
            id = UUID.randomUUID().toString(),
            characterId = character.id,
            characterName = character.name,
            relationshipStage = options.relationshipStage,
            refId = ref.id,
            instructionBranch = ref.name,
            instructions = instructionRenderer.render(ref.instructions, character, options),
            model = request.model?.takeIf { it.isNotBlank() } ?: defaultModel,
            situationNote = options.situationNote,
            includeVoiceSamples = options.includeVoiceSamples,
            closedTopicMode = options.closedTopicMode,
            createdAt = OffsetDateTime.now(),
        )
        sessionStore.put(session)
        return session.toDto(expiresAt(session))
    }

    fun getSession(sessionId: String): CharacterChatSessionDto {
        val session = findSession(sessionId)
        return session.toDto(expiresAt(session))
    }

    /**
     * 사용자 메시지를 보내고 응답을 SSE로 흘려보낸다.
     *
     * 세션 조회, 동시 턴 검사, 실행 용량 확보는 요청 스레드에서 먼저 끝낸다.
     * 스트림이 열린 뒤에는 예외를 HTTP 상태로 바꿀 수 없기 때문이다.
     */
    fun sendMessage(sessionId: String, request: CharacterChatMessageRequest): SseEmitter {
        val session = findSession(sessionId)
        if (!session.tryBeginTurn()) {
            throw ResourceConflictException("이미 응답을 생성 중인 세션입니다: $sessionId")
        }
        if (!permits.tryAcquire()) {
            session.endTurn()
            throw ChatSessionCapacityExceededException("동시에 진행할 수 있는 대화 수를 초과했습니다")
        }

        val emitter = SseEmitter(streamTimeout.toMillis())
        try {
            executorService.execute { streamTurn(session, request.content, emitter) }
        } catch (failure: RuntimeException) {
            permits.release()
            session.endTurn()
            throw failure
        }
        return emitter
    }

    private fun streamTurn(session: CharacterChatSession, content: String, emitter: SseEmitter) {
        val sb = StringBuilder("nowTime: ${OffsetDateTime.now().format(datetimeFormatter)} ")
        sb.append("user: $content")
        val userMessage = CharacterChatMessage(
            role = ChatRole.USER,
            content = sb.toString(),
            createdAt = OffsetDateTime.now(),
        )
        try {
            val reply = StringBuilder()
            chatLlmClient.streamReply(
                CharacterChatStreamRequest(
                    model = session.model,
                    instructions = session.instructions,
                    messages = session.messages() + userMessage,
                ),
            ) { delta ->
                reply.append(delta)
                emitter.send(
                    SseEmitter.event()
                        .name(EVENT_DELTA)
                        .data(CharacterChatDeltaEventDto(text = delta), MediaType.APPLICATION_JSON),
                )
            }

            // 사용자 메시지와 응답은 스트림이 끝까지 성공한 뒤에만 함께 기록한다.
            val assistantMessage = CharacterChatMessage(
                role = ChatRole.ASSISTANT,
                content = reply.toString(),
                createdAt = OffsetDateTime.now(),
            )
            val messageIndex = session.appendTurn(userMessage, assistantMessage)
            session.touch()
            emitter.send(
                SseEmitter.event()
                    .name(EVENT_DONE)
                    .data(
                        CharacterChatDoneEventDto(content = assistantMessage.content, messageIndex = messageIndex),
                        MediaType.APPLICATION_JSON,
                    ),
            )
            emitter.complete()
        } catch (failure: Exception) {
            log.warn("Character chat stream failed. sessionId={}", session.id, failure)
            sendErrorEvent(emitter, failure)
        } finally {
            permits.release()
            session.endTurn()
        }
    }

    /**
     * 스트림이 열린 뒤의 실패는 [ltmp.charactergen.api.ApiExceptionHandler]가 처리할 수 없으므로
     * 같은 형태의 payload를 이벤트로 보내고 스트림을 정상 종료한다.
     * completeWithError로 끊으면 클라이언트가 이 payload를 받지 못한다.
     */
    private fun sendErrorEvent(emitter: SseEmitter, failure: Exception) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(EVENT_ERROR)
                    .data(
                        ApiErrorResponse(
                            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            code = "CHAT_STREAM_FAILED",
                            message = failure.message ?: failure.javaClass.simpleName,
                            timestamp = OffsetDateTime.now(),
                        ),
                        MediaType.APPLICATION_JSON,
                    ),
            )
            emitter.complete()
        } catch (sendFailure: Exception) {
            // 이미 끊긴 연결이면 더 할 수 있는 게 없다.
            log.debug("Failed to deliver chat error event", sendFailure)
            emitter.completeWithError(failure)
        }
    }

    private fun findSession(sessionId: String): CharacterChatSession =
        sessionStore.find(sessionId)
            ?: throw ResourceNotFoundException("대화 세션이 없거나 만료되었습니다: $sessionId")

    private fun expiresAt(session: CharacterChatSession): OffsetDateTime =
        session.lastAccessedAt.plus(sessionStore.ttl()).atZone(ZoneId.systemDefault()).toOffsetDateTime()

    @PreDestroy
    fun shutdown() {
        executorService.shutdown()
    }

    /**
     * 대화에 사용할 런타임 인스트럭션 ref를 결정한다.
     *
     * refId를 지정하면 용도가 CHARACTER_RUNTIME인 템플릿의 ref인지 검증하고,
     * instructionBranch를 지정하면 기본 CHARACTER_RUNTIME 템플릿 안에서 그 브랜치를 찾는다.
     * 둘 다 없으면 기본 프롬프트를 쓴다.
     */
    private fun resolveRuntimeRef(refId: Long?, instructionBranch: String?): PromptTemplateRef {
        if (refId != null && instructionBranch != null) {
            throw InvalidRequestException("refId와 instructionBranch는 동시에 지정할 수 없습니다.")
        }
        if (refId != null) {
            val ref = promptTemplateRefRepository.findOneWithDetailsById(refId)
                ?: throw ResourceNotFoundException("PromptTemplateRef not found: $refId")
            if (ref.promptTemplate.type != PromptTemplateType.CHARACTER_RUNTIME) {
                throw InvalidRequestException(
                    "PromptTemplate type mismatch. expected=${PromptTemplateType.CHARACTER_RUNTIME}, " +
                        "actual=${ref.promptTemplate.type}, refId=$refId",
                )
            }
            return ref
        }
        if (instructionBranch != null) {
            val template = defaultPromptTemplateService.resolveDefaultTemplate(PromptTemplateType.CHARACTER_RUNTIME)
            return promptTemplateRefRepository
                .findOneWithDetailsByPromptTemplateIdAndName(template.id, instructionBranch)
                ?: throw ResourceNotFoundException(
                    "PromptTemplateRef branch not found: templateId=${template.id}, branchName=$instructionBranch",
                )
        }
        return defaultPromptTemplateService.resolveDefaultRef(PromptTemplateType.CHARACTER_RUNTIME)
    }

    private companion object {
        const val EVENT_DELTA = "delta"
        const val EVENT_DONE = "done"
        const val EVENT_ERROR = "error"

        const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"
    }
}
