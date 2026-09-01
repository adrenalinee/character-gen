package ltmp.charactergen.api.chat

import ltmp.charactergen.client.openai.CharacterChatLlmClient
import ltmp.charactergen.client.openai.CharacterChatStreamRequest
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.Character
import ltmp.charactergen.data.entity.L0GenerationExecute
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.service.DefaultPromptTemplateService
import ltmp.charactergen.service.chat.CharacterChatSessionStore
import ltmp.charactergen.service.chat.ChatRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@Import(CharacterChatIntegrationTests.FakeChatLlmConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
class CharacterChatIntegrationTests(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val characterRepository: CharacterRepository,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val defaultPromptTemplateService: DefaultPromptTemplateService,
    private val sessionStore: CharacterChatSessionStore,
    private val fakeChatLlmClient: FakeCharacterChatLlmClient,
) {
    @BeforeEach
    fun resetState() {
        fakeChatLlmClient.reset()
        // 세션 스토어는 테스트 사이에 공유되는 싱글턴이라 직접 비운다.
        sessionStore.clear()
    }

    @Test
    fun `세션을 만들면 조립된 인스트럭션과 함께 반환한다`() {
        val characterId = persistCharacter()

        val response = createSession(characterId, "EARLY")

        assertEquals(characterId.toInt(), response.path("characterId").asInt())
        assertEquals("서윤", response.path("characterName").asString())
        assertEquals("EARLY", response.path("relationshipStage").asString())
        // 개방 층위를 주지 않으면 단계에서 파생한다
        assertEquals("SURFACE", response.path("openedDisclosureLevel").asString())
        assertEquals("main", response.path("instructionBranch").asString())
        assertEquals("TOPIC_ONLY", response.path("closedTopicMode").asString())
        assertTrue(response.path("messages").isEmpty)
        assertTrue(response.path("sessionId").asString().isNotBlank())

        val instructions = response.path("instructions").asString()
        assertContains(instructions, "너는 서윤이다")
        assertContains(instructions, "먼저 묻지 않는다")
    }

    @Test
    fun `런타임 인스트럭션 조회는 세션을 만들지 않고 세션 생성과 같은 전문을 돌려준다`() {
        val characterId = persistCharacter()

        val preview = objectMapper.readTree(
            mockMvc.post("/api/v1/character-chat/runtime-instructions") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"characterId":$characterId,"relationshipStage":"EARLY"}"""
            }.andExpect {
                status { isOk() }
            }.andReturn().response.contentAsString,
        )

        assertEquals(characterId.toInt(), preview.path("characterId").asInt())
        assertEquals("서윤", preview.path("characterName").asString())
        assertEquals("EARLY", preview.path("relationshipStage").asString())
        assertEquals("SURFACE", preview.path("openedDisclosureLevel").asString())
        assertEquals("main", preview.path("instructionBranch").asString())
        assertEquals("TOPIC_ONLY", preview.path("closedTopicMode").asString())
        assertTrue(preview.path("sessionId").isMissingNode, "조회 결과에는 세션이 없다")

        val session = createSession(characterId, "EARLY")
        assertEquals(
            session.path("instructions").asString(),
            preview.path("instructions").asString(),
            "조회한 전문은 같은 요청으로 만든 세션에 주입되는 전문과 같아야 한다",
        )
    }

    @Test
    fun `관계 단계에 따라 조립되는 인스트럭션이 달라진다`() {
        val characterId = persistCharacter()

        val early = createSession(characterId, "EARLY").path("instructions").asString()
        val close = createSession(characterId, "CLOSE").path("instructions").asString()

        assertContains(early, "먼저 묻지 않는다")
        assertFalse(early.contains("교체 경험"), "얕은 관계에서 심층 개인사가 새면 안 된다")
        assertContains(close, "거리를 유지한 채 곁에 둔다")
        assertTrue(close.contains("교체 경험"), "가까운 관계에서는 심층 개인사가 열려야 한다")
    }

    @Test
    fun `개방 층위를 직접 지정하면 관계 단계와 무관하게 그 층위까지 연다`() {
        val characterId = persistCharacter()

        val response = objectMapper.readTree(
            mockMvc.post("/api/v1/character-chat/sessions") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """
                    {"characterId":$characterId,"relationshipStage":"EARLY","openedDisclosureLevel":"DEEP"}
                """.trimIndent()
            }.andReturn().response.contentAsString,
        )

        assertEquals("EARLY", response.path("relationshipStage").asString())
        assertEquals("DEEP", response.path("openedDisclosureLevel").asString())
        val instructions = response.path("instructions").asString()
        assertContains(instructions, "먼저 묻지 않는다")
        assertContains(instructions, "교체 경험")
    }

    @Test
    fun `상황 부여와 예시 발화 토글이 인스트럭션에 반영된다`() {
        val characterId = persistCharacter()

        val body = """
            {
              "characterId": $characterId,
              "relationshipStage": "EARLY",
              "situationNote": "오늘 원고를 크게 반려당했다",
              "includeVoiceSamples": false,
              "closedTopicMode": "OMIT"
            }
        """.trimIndent()
        val response = objectMapper.readTree(
            mockMvc.post("/api/v1/character-chat/sessions") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = body
            }.andReturn().response.contentAsString,
        )

        val instructions = response.path("instructions").asString()
        assertContains(instructions, "## 지금 네 상황")
        assertContains(instructions, "오늘 원고를 크게 반려당했다")
        assertFalse(instructions.contains("## 톤 참조"))
        assertFalse(instructions.contains("## 말하지 않는 것"))
        assertEquals("OMIT", response.path("closedTopicMode").asString())
    }

    @Test
    fun `대화 요청은 delta 이벤트를 흘려보내고 done으로 끝난다`() {
        val sessionId = createSession(persistCharacter(), "EARLY").path("sessionId").asString()
        fakeChatLlmClient.deltas = listOf("안녕", "하세요.")

        val body = sendMessage(sessionId, "안녕하세요")

        assertContains(body, "event:delta")
        assertContains(body, "\"text\":\"안녕\"")
        assertContains(body, "\"text\":\"하세요.\"")
        assertContains(body, "event:done")
        assertContains(body, "\"content\":\"안녕하세요.\"")
        assertFalse(body.contains("event:error"))
    }

    @Test
    fun `대화 기록 조회로 오간 메시지를 순서대로 확인할 수 있다`() {
        val sessionId = createSession(persistCharacter(), "EARLY").path("sessionId").asString()
        fakeChatLlmClient.deltas = listOf("네.")
        sendMessage(sessionId, "안녕하세요")

        val messages = objectMapper.readTree(
            mockMvc.get("/api/v1/character-chat/sessions/$sessionId").andReturn().response.contentAsString,
        ).path("messages")

        assertEquals(2, messages.size())
        assertEquals(ChatRole.USER.name, messages[0].path("role").asString())
        // 사용자 메시지 앞에는 전송 시각이 붙는다. 시각은 매번 달라지므로 뒷부분만 확인한다.
        assertTrue(messages[0].path("content").asString().endsWith("user: 안녕하세요"))
        assertEquals(ChatRole.ASSISTANT.name, messages[1].path("role").asString())
        assertEquals("네.", messages[1].path("content").asString())
    }

    @Test
    fun `두 번째 턴에는 앞 턴의 대화가 함께 전달된다`() {
        val sessionId = createSession(persistCharacter(), "EARLY").path("sessionId").asString()
        fakeChatLlmClient.deltas = listOf("네.")
        sendMessage(sessionId, "안녕하세요")
        fakeChatLlmClient.deltas = listOf("그렇군요.")
        sendMessage(sessionId, "오늘 어땠어요?")

        val lastRequest = requireNotNull(fakeChatLlmClient.lastRequest)
        assertEquals(3, lastRequest.messages.size)
        assertTrue(lastRequest.messages[0].content.endsWith("user: 안녕하세요"))
        assertEquals("네.", lastRequest.messages[1].content)
        assertTrue(lastRequest.messages[2].content.endsWith("user: 오늘 어땠어요?"))

        // 인스트럭션은 매 턴 다시 전달된다.
        assertContains(lastRequest.instructions, "너는 서윤이다")
    }

    @Test
    fun `스트림이 실패하면 error 이벤트를 보내고 그 턴은 기록하지 않는다`() {
        val sessionId = createSession(persistCharacter(), "EARLY").path("sessionId").asString()
        fakeChatLlmClient.failure = IllegalStateException("모델 호출 실패")

        val body = sendMessage(sessionId, "안녕하세요")

        assertContains(body, "event:error")
        assertContains(body, "CHAT_STREAM_FAILED")
        assertContains(body, "모델 호출 실패")

        val messages = objectMapper.readTree(
            mockMvc.get("/api/v1/character-chat/sessions/$sessionId").andReturn().response.contentAsString,
        ).path("messages")
        assertTrue(messages.isEmpty, "실패한 턴은 대화 기록에 남으면 안 된다")
    }

    @Test
    fun `없는 세션으로 대화하거나 조회하면 404를 반환한다`() {
        mockMvc.get("/api/v1/character-chat/sessions/없는세션").andExpect {
            status { isNotFound() }
        }
        mockMvc.post("/api/v1/character-chat/sessions/없는세션/messages") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"content":"안녕하세요"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `없는 캐릭터로 세션을 만들면 404를 반환한다`() {
        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"characterId":999999,"relationshipStage":"EARLY"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `없는 인스트럭션 브랜치를 지정하면 404를 반환한다`() {
        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {"characterId":${persistCharacter()},"relationshipStage":"EARLY","instructionBranch":"없는브랜치"}
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `빈 메시지는 400을 반환한다`() {
        val sessionId = createSession(persistCharacter(), "EARLY").path("sessionId").asString()

        mockMvc.post("/api/v1/character-chat/sessions/$sessionId/messages") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"content":"   "}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `A_B 비교용 브랜치를 지정하면 그 브랜치의 템플릿으로 조립한다`() {
        val characterId = persistCharacter()
        val template = defaultPromptTemplateService.resolveDefaultTemplate(PromptTemplateType.CHARACTER_RUNTIME)
        promptTemplateRefRepository.saveAndFlush(
            PromptTemplateRef(
                promptTemplate = template,
                type = PromptTemplateRefType.BRANCH,
                name = "variant-a",
                instructions = "실험 템플릿. 이름은 {{NAME}}이고 호칭은 {{STAGE_ADDRESS}}이다.",
            ),
        )

        val response = objectMapper.readTree(
            mockMvc.post("/api/v1/character-chat/sessions") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """
                    {"characterId":$characterId,"relationshipStage":"EARLY","instructionBranch":"variant-a"}
                """.trimIndent()
            }.andReturn().response.contentAsString,
        )

        assertEquals("variant-a", response.path("instructionBranch").asString())
        assertEquals(
            "실험 템플릿. 이름은 서윤이고 호칭은 기본적으로 존댓말이다.",
            response.path("instructions").asString(),
        )
    }

    @Test
    fun `refId로 다른 런타임 브랜치를 지정하면 그 ref로 조립한다`() {
        val characterId = persistCharacter()
        val template = defaultPromptTemplateService.resolveDefaultTemplate(PromptTemplateType.CHARACTER_RUNTIME)
        val ref = promptTemplateRefRepository.saveAndFlush(
            PromptTemplateRef(
                promptTemplate = template,
                type = PromptTemplateRefType.BRANCH,
                name = "variant-b",
                instructions = "refId로 고른 템플릿. 이름은 {{NAME}}이다.",
            ),
        )

        val response = objectMapper.readTree(
            mockMvc.post("/api/v1/character-chat/sessions") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"characterId":$characterId,"relationshipStage":"EARLY","refId":${ref.id}}"""
            }.andExpect { status { isCreated() } }.andReturn().response.contentAsString,
        )

        assertEquals(ref.id.toInt(), response.path("refId").asInt())
        assertEquals("variant-b", response.path("instructionBranch").asString())
        assertEquals("refId로 고른 템플릿. 이름은 서윤이다.", response.path("instructions").asString())
    }

    @Test
    fun `용도가 다른 ref를 지정하면 400을 반환한다`() {
        val characterId = persistCharacter()
        val generationTemplate = promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "생성 전용 ${counter++}", type = PromptTemplateType.CHARACTER_GENERATION),
        )
        val ref = promptTemplateRefRepository.saveAndFlush(
            PromptTemplateRef(
                promptTemplate = generationTemplate,
                type = PromptTemplateRefType.BRANCH,
                name = "main",
                instructions = "캐릭터 생성 지시",
            ),
        )

        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"characterId":$characterId,"relationshipStage":"EARLY","refId":${ref.id}}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `refId와 instructionBranch를 동시에 지정하면 400을 반환한다`() {
        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {"characterId":${persistCharacter()},"relationshipStage":"EARLY","refId":1,"instructionBranch":"main"}
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `유지 가능한 세션 수를 넘기면 429를 반환한다`() {
        val characterId = persistCharacter()
        repeat(SESSION_MAX_COUNT) { createSession(characterId, "EARLY") }

        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"characterId":$characterId,"relationshipStage":"EARLY"}"""
        }.andExpect {
            status { isTooManyRequests() }
            jsonPath("$.code") { value("CHAT_SESSION_CAPACITY_EXCEEDED") }
        }
    }

    private fun createSession(characterId: Long, stage: String) = objectMapper.readTree(
        mockMvc.post("/api/v1/character-chat/sessions") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"characterId":$characterId,"relationshipStage":"$stage"}"""
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString,
    )

    /**
     * SSE 응답은 비동기 스레드가 채우므로 스트림이 끝날 때까지 기다린 뒤 본문을 읽는다.
     */
    private fun sendMessage(sessionId: String, content: String): String {
        val result: MvcResult = mockMvc.post("/api/v1/character-chat/sessions/$sessionId/messages") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            this.content = """{"content":"$content"}"""
        }.andExpect {
            request { asyncStarted() }
        }.andReturn()

        result.asyncResult
        return result.response.contentAsByteArray.toString(Charsets.UTF_8)
    }

    private fun persistCharacter(): Long {
        val template = promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "chat test ${counter++}", type = PromptTemplateType.CHARACTER_GENERATION),
        )
        val ref = promptTemplateRefRepository.saveAndFlush(
            PromptTemplateRef(
                promptTemplate = template,
                type = PromptTemplateRefType.BRANCH,
                name = "main",
                instructions = "test",
            ),
        )
        val execute = generationExecuteRepository.saveAndFlush(
            L0GenerationExecute(
                promptTemplateRef = ref,
                userInputJson = "{}",
                status = L0GenerationExecuteStatus.COMPLETED,
            ),
        )
        val character = characterRepository.saveAndFlush(chatTestCharacter(execute))
        execute.character = character
        generationExecuteRepository.flush()
        return character.id
    }

    @TestConfiguration
    class FakeChatLlmConfiguration {
        @Bean
        @Primary
        fun fakeChatLlmClient(): FakeCharacterChatLlmClient = FakeCharacterChatLlmClient()
    }

    class FakeCharacterChatLlmClient : CharacterChatLlmClient {
        var deltas: List<String> = emptyList()
        var failure: RuntimeException? = null
        var lastRequest: CharacterChatStreamRequest? = null

        fun reset() {
            deltas = emptyList()
            failure = null
            lastRequest = null
        }

        override fun streamReply(request: CharacterChatStreamRequest, onDelta: (String) -> Unit) {
            lastRequest = request
            failure?.let { throw it }
            deltas.forEach(onDelta)
        }
    }

    private companion object {
        /** character-chat.session.max-count 설정값 */
        const val SESSION_MAX_COUNT = 100
        var counter = 0
    }
}

/**
 * 인스트럭션 조립 결과를 확인할 수 있게 값이 분명한 캐릭터를 만든다.
 */
private fun chatTestCharacter(execute: L0GenerationExecute): Character = Character(
    generationExecute = execute,
    name = "서윤",
    age = 29,
    gender = "여성",
    occupation = "웹툰 작가",
    dailyLife = "밤에만 작업한다",
    oneLine = "선을 지킨다",
    lieType = "대체 가능성 공포형",
    lieStatement = "나는 대체된다",
    lieWound = "첫 연재에서 교체됐다",
    want = "자기 방식의 작품을 남긴다",
    need = "교체와 가치가 무관함을 안다",
    wantNeedConflict = "인정을 좇을수록 자기 방식을 버린다",
    contradictionTraitA = "먼저 선을 긋는다",
    contradictionTraitB = "떠나면 붙잡는다",
    contradictionSharedRoot = "대체될 두려움",
    onRejection = "한 번 물러난 뒤 다시 묻지 않는다",
    onConflict = "근거를 한 줄로 말하고 물러서지 않는다",
    onAbsence = "없었던 일처럼 작업 이야기부터 잇는다",
    onOverApproach = "속도를 늦추고 하던 이야기로 돌아온다",
    emotionalExpressionNote = "감정을 사실로 바꾼다",
    avoidanceNote = "한 문장 뒤 침묵한다",
    speechArchetypeNote = "작화 도구에 빗댄다",
    sentencePattern = "짧은 결론 뒤 비유",
    sentencePatternRuntimeIncluded = true,
    recognizability = "작화 도구 비유",
    longTermViability = "교체 공포가 반복된다",
    imbalanceNote = "없음",
    traitReflection = "낯선 방식에서 행동을 만들지 않는다",
    concreteDetails = listOf(
        ltmp.charactergen.data.entity.CharacterConcreteDetailEntity(detailText = "왼손잡이", runtimeIncluded = true),
    ),
    behavioralRules = listOf(
        ltmp.charactergen.data.entity.CharacterBehavioralRuleEntity(
            ruleText = "칭찬을 받으면 다음 작업 이야기로 넘긴다",
        ),
    ),
    relationshipStages = listOf(
        chatTestRelationshipStage(ltmp.charactergen.data.RelationshipStage.EARLY, "먼저 묻지 않는다"),
        chatTestRelationshipStage(ltmp.charactergen.data.RelationshipStage.DEEPENING, "작업 이야기만 연다"),
        chatTestRelationshipStage(ltmp.charactergen.data.RelationshipStage.CLOSE, "거리를 유지한 채 곁에 둔다"),
    ),
    nonNegotiables = listOf(
        ltmp.charactergen.data.entity.CharacterNonNegotiableEntity(
            domain = ltmp.charactergen.model.NonNegotiableDomain.`관계를 다루는 방식`,
            statement = "비교하지 않는다",
            tieToLie = "교체 공포",
            userFacing = true,
        ),
    ),
    forbiddenBehaviors = listOf(
        ltmp.charactergen.data.entity.CharacterForbiddenBehaviorEntity(
            behavior = "상대를 시험하지 않는다",
            tieToLie = "교체 공포",
        ),
    ),
    verbalTics = listOf(
        ltmp.charactergen.data.entity.CharacterVerbalTicEntity(
            situation = "거절할 때",
            description = "선을 다시 긋는다고 말한다",
            runtimeIncluded = true,
        ),
    ),
    bannedWords = listOf(
        ltmp.charactergen.data.entity.CharacterBannedWordEntity(
            source = ltmp.charactergen.data.BannedWordSource.FROM_LIE,
            wordText = "무조건",
        ),
    ),
    relations = listOf(
        ltmp.charactergen.data.entity.CharacterRelationEntity(
            slot = ltmp.charactergen.model.RelationSlot.`과거`,
            name = "민호",
            relation = "전 편집자",
            description = "첫 연재에서 교체를 알렸다",
            presence = ltmp.charactergen.model.RelationPresence.`부재`,
            tieToLie = "대체 공포의 기원",
        ),
    ),
    disclosureItems = listOf(
        ltmp.charactergen.data.entity.CharacterDisclosureItemEntity(
            level = ltmp.charactergen.data.DisclosureLevel.SURFACE,
            itemText = "마감 습관",
            runtimeIncluded = true,
        ),
        ltmp.charactergen.data.entity.CharacterDisclosureItemEntity(
            level = ltmp.charactergen.data.DisclosureLevel.DEEP,
            itemText = "교체 경험",
            runtimeIncluded = true,
        ),
    ),
    voiceSamples = listOf(
        ltmp.charactergen.data.entity.CharacterVoiceSampleEntity(
            situation = "의견에 반대했을 때",
            line = "그 선은 제가 정할게요.",
            demonstrates = "거리 유지",
        ),
    ),
)

private fun chatTestRelationshipStage(
    stage: ltmp.charactergen.data.RelationshipStage,
    attachment: String,
) = ltmp.charactergen.data.entity.CharacterRelationshipStageEntity(
    stage = stage,
    attachment = attachment,
    address = "기본적으로 존댓말",
    emotionalRange = "불편함만 짧게 드러낸다",
    avoidance = "과거를 물으면 한 문장으로 끊는다",
)
