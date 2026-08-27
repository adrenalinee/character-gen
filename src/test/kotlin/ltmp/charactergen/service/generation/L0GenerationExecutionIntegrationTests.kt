package ltmp.charactergen.service.generation

import ltmp.charactergen.api.L0CharacterGenerationRequest
import ltmp.charactergen.l0GenerationTestOptions
import ltmp.charactergen.client.openai.FakeTextEmbeddingClient
import ltmp.charactergen.client.openai.L0CharacterLlmClient
import ltmp.charactergen.client.openai.L0CharacterLlmResult
import ltmp.charactergen.client.openai.TextEmbeddingClient
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.Gender
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.CharacterRepository
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import ltmp.charactergen.data.repo.VerbalTicSituationRepository
import ltmp.charactergen.data.repo.WoundAgentRepository
import ltmp.charactergen.data.repo.WoundDurationRepository
import ltmp.charactergen.data.repo.WoundFormRepository
import ltmp.charactergen.data.repo.WoundPlaceRepository
import ltmp.charactergen.data.repo.WoundTimingRepository
import ltmp.charactergen.model.L0CharacterResponse
import ltmp.charactergen.service.L0CharacterGenerationService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import ltmp.charactergen.service.InvalidRequestException

@SpringBootTest
@AutoConfigureMockMvc
@Import(L0GenerationExecutionIntegrationTests.FakeLlmConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
class L0GenerationExecutionIntegrationTests(
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val generationService: L0CharacterGenerationService,
    private val executor: L0GenerationExecutor,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val characterRepository: CharacterRepository,
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val woundTimingRepository: WoundTimingRepository,
    private val woundPlaceRepository: WoundPlaceRepository,
    private val woundAgentRepository: WoundAgentRepository,
    private val woundFormRepository: WoundFormRepository,
    private val woundDurationRepository: WoundDurationRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
    private val verbalTicSituationRepository: VerbalTicSituationRepository,
    private val fakeLlmClient: FakeL0CharacterLlmClient,
    private val objectMapper: ObjectMapper,
    private val mockMvc: MockMvc,
) {
    @Test
    fun `PREPARING 실행을 비동기 executor 작업 단위로 처리하고 캐릭터를 저장한다`() {
        val template = promptTemplateRepository.save(PromptTemplate(name = "L0 execution test", type = PromptTemplateType.CHARACTER_GENERATION))
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system {{UNKNOWN_SYSTEM_VALUE}}",
        ).also {
            it.addInput(
                "user",
                "{{NAME}}|{{DOMAIN}}|{{LIE_STATEMENT}}|{{WOUND_TIMING}}|{{WOUND_PLACE}}|" +
                    "{{WOUND_AGENT}}|{{WOUND_FORM}}|{{WOUND_DURATION}}|{{O_DESC}}|{{ATTACHMENT_DESC}}|" +
                    "{{SPEECH_REGISTER}}|{{TIC_SITUATIONS}}|{{EXTRA_CONSTRAINTS}}",
            )
        }
        promptTemplateRefRepository.saveAndFlush(ref)
        fakeLlmClient.failure = null
        fakeLlmClient.response = objectMapper.readValue(RESPONSE_JSON, L0CharacterResponse::class.java)

        val generation = generationService.requestGeneration(generationRequest(ref.id))
        assertEquals(L0GenerationExecuteStatus.PREPARING, generation.status)

        executor.execute(generation.id)

        val completed = generationExecuteRepository.findById(generation.id).orElseThrow()
        assertEquals(L0GenerationExecuteStatus.COMPLETED, completed.status)
        assertEquals(RESPONSE_JSON, completed.llmOutput)
        assertNotNull(completed.completedAt)
        assertNotNull(completed.elapsed)
        assertEquals("캐릭터 생성 완료", completed.message)
        val selectedOptions = testOptions()
        assertEquals(selectedOptions.domainId, completed.domain?.id)
        assertEquals(selectedOptions.lieId, completed.lie?.id)
        assertEquals("서윤", completed.name)
        assertEquals("유년", completed.woundTiming?.value)
        assertEquals("가정", completed.woundPlace?.value)
        assertEquals("부모", completed.woundAgent?.value)
        assertEquals("사고", completed.woundForm?.value)
        assertEquals("한순간", completed.woundDuration?.value)
        assertEquals(selectedOptions.opennessStageId, completed.opennessStage?.id)
        assertEquals(selectedOptions.conscientiousnessStageId, completed.conscientiousnessStage?.id)
        assertEquals(selectedOptions.extraversionStageId, completed.extraversionStage?.id)
        assertEquals(selectedOptions.agreeablenessStageId, completed.agreeablenessStage?.id)
        assertEquals(selectedOptions.neuroticismStageId, completed.neuroticismStage?.id)
        assertEquals(selectedOptions.attachmentStyleId, completed.attachmentStyle?.id)
        assertEquals(selectedOptions.emotionalExpressionId, completed.emotionalExpression?.id)
        assertEquals(selectedOptions.avoidanceStyleId, completed.avoidanceStyle?.id)
        assertEquals(selectedOptions.speechArchetypeId, completed.speechArchetype?.id)
        assertEquals(selectedOptions.speechRegisterId, completed.speechRegister?.id)
        assertEquals(29, completed.age)
        assertEquals(Gender.FEMALE, completed.gender)
        assertEquals(2, completed.scale)
        assertEquals(4, completed.emotion)

        val character = characterRepository.findAll().single { it.generationExecute.id == generation.id }
        assertEquals("서윤", character.name)
        assertEquals(3, character.concreteDetails.size)
        // "마감 습관"은 disclosure_ladder.surface와 글자까지 같아 런타임에서 빠진다. (v4.1 B-10)
        assertEquals(
            listOf("왼손잡이" to true, "밤에 작업" to true, "마감 습관" to false),
            character.concreteDetails.map { it.detailText to it.runtimeIncluded },
        )
        assertEquals(2, character.behavioralRules.size)
        assertEquals(3, character.relationshipStages.size)
        assertEquals(3, character.nonNegotiables.size)
        assertEquals(2, character.forbiddenBehaviors.size)
        assertEquals(1, character.verbalTics.size)
        assertEquals(2, character.bannedWords.size)
        assertEquals(1, character.relations.size)
        assertEquals(4, character.disclosureItems.size)
        assertEquals(1, character.voiceSamples.size)
        assertEquals(character.id, completed.character?.id)

        mockMvc.get("/api/v1/l0-characters/generations/{generationId}", generation.id).andExpect {
            status { isOk() }
            jsonPath("$.characterId") { value(character.id) }
        }

        mockMvc.get("/api/v1/l0-characters") {
            param("name", "서")
            param("generationExecuteId", generation.id.toString())
            param("page", "0")
            param("size", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(character.id) }
            jsonPath("$.content[0].generationExecuteId") { value(generation.id) }
            jsonPath("$.content[0].name") { value("서윤") }
            jsonPath("$.content[0].concreteDetails") { doesNotExist() }
            jsonPath("$.content[0].dailyLife") { doesNotExist() }
            jsonPath("$.page.totalElements") { value(1) }
        }

        mockMvc.get("/api/v1/l0-characters/{characterId}", character.id).andExpect {
            status { isOk() }
            jsonPath("$.generationExecuteId") { value(generation.id) }
            jsonPath("$.name") { value("서윤") }
            jsonPath("$.dailyLife") { value("작업한다") }
            jsonPath("$.lieType") { value("대체 가능성 공포형") }
            jsonPath("$.contradictionTraitA") { value("먼저 선을 긋는다") }
            jsonPath("$.speechArchetypeNote") { value("작화 도구에 빗댄다") }
            jsonPath("$.onConflict") { value("근거를 한 줄로 말하고 물러서지 않는다") }
            jsonPath("$.onOverApproach") { value("속도를 늦추고 하던 이야기로 돌아온다") }
            jsonPath("$.behavioralRules.length()") { value(2) }
            jsonPath("$.behavioralRules[0].ruleText") { value("칭찬을 받으면 다음 작업 이야기로 넘긴다") }
            jsonPath("$.relationshipStages.length()") { value(3) }
            jsonPath("$.relationshipStages[0].stage") { value("EARLY") }
            jsonPath("$.relationshipStages[0].address") { value("이름 없이 존댓말로 부른다") }
            jsonPath("$.relationshipStages[2].stage") { value("CLOSE") }
            jsonPath("$.longTermViability") { value("여러 관계에서 교체 공포가 반복된다") }
            jsonPath("$.identity") { doesNotExist() }
            jsonPath("$.core") { doesNotExist() }
            jsonPath("$.voice") { doesNotExist() }
            jsonPath("$.concreteDetails.length()") { value(3) }
            jsonPath("$.concreteDetails[0].id") { isNumber() }
            jsonPath("$.concreteDetails[0].detailText") { value("왼손잡이") }
            jsonPath("$.concreteDetails[0].runtimeIncluded") { value(true) }
            // surface와 겹친다고 판정된 항목은 저장은 되지만 런타임에는 들어가지 않는다. (v4.1 B-10)
            jsonPath("$.concreteDetails[2].detailText") { value("마감 습관") }
            jsonPath("$.concreteDetails[2].runtimeIncluded") { value(false) }
            jsonPath("$.nonNegotiables.length()") { value(3) }
            jsonPath("$.bannedWords.length()") { value(2) }
            jsonPath("$.bannedWords[0].source") { value("FROM_LIE") }
            jsonPath("$.bannedWords[0].wordText") { value("무조건") }
            jsonPath("$.bannedWords[1].source") { value("FROM_BACKGROUND") }
            jsonPath("$.bannedWords[1].wordText") { value("당연히") }
            jsonPath("$.relations[0].name") { value("민호") }
            jsonPath("$.disclosureItems[3].level") { value("DEEPEST") }
            jsonPath("$.disclosureItems[3].itemText") { value("자기 가치에 대한 공포") }
            jsonPath("$.voiceSamples[0].line") { value("그 선은 제가 정할게요.") }
        }

        mockMvc.delete("/api/v1/l0-characters/{characterId}", character.id).andExpect {
            status { isNoContent() }
        }
        assertFalse(characterRepository.existsById(character.id))
        assertTrue(generationExecuteRepository.existsById(generation.id))
        mockMvc.get("/api/v1/l0-characters/{characterId}", character.id).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
        }

        assertEquals("system ", fakeLlmClient.lastPrompt.instructions)
        val renderedInput = fakeLlmClient.lastPrompt.inputs.single().inputText
        assertTrue(renderedInput.contains("웹툰"))
        assertTrue(renderedInput.contains("서윤"))
        assertTrue(renderedInput.contains("유년"))
        assertTrue(renderedInput.contains("가정"))
        assertTrue(renderedInput.contains("부모"))
        assertTrue(renderedInput.contains("사고"))
        assertTrue(renderedInput.contains("한순간"))
        assertTrue(renderedInput.contains("나는 언제든 다른 사람으로 대체된다"))
        assertTrue(renderedInput.contains("이 축은 이 인물을 설명하는 특징이 아니다"))
        assertTrue(renderedInput.contains("누구와도 비슷한 거리를 두고"))
        assertTrue(renderedInput.contains("존댓말 기본"))
        assertFalse(renderedInput.contains("{{"))

        // 말버릇 상황은 모델이 아니라 서버가 카탈로그에서 골라 넣는다 (v4 §4.1)
        val injectedSituations = verbalTicSituationRepository.findAllByOrderByIdAsc()
            .filter { renderedInput.contains("- ${it.situation}") }
        assertEquals(3, injectedSituations.size)
    }

    @Test
    fun `이미 생성된 캐릭터를 프롬프트에 알려주고 빈 목록을 주면 알려주지 않는다`() {
        val ref = existingCharacterPromptRef("L0 existing character test")
        fakeLlmClient.failure = null
        fakeLlmClient.response = objectMapper.readValue(RESPONSE_JSON, L0CharacterResponse::class.java)

        // 첫 실행으로 기존 캐릭터를 하나 만든다
        val first = generationService.requestGeneration(generationRequest(ref.id))
        executor.execute(first.id)

        // 지정하지 않으면 최근 캐릭터를 서버가 붙인다
        val second = generationService.requestGeneration(generationRequest(ref.id))
        executor.execute(second.id)
        val rendered = fakeLlmClient.lastPrompt.inputs.single().inputText
        assertTrue(rendered.contains("서윤 / 웹툰 / 대체 가능성 공포형 / 첫 연재에서 교체됐다 / 29세 / 안 쓰는 말: 무조건, 당연히"), rendered)
        assertEquals(
            "서윤 / 웹툰 / 대체 가능성 공포형 / 첫 연재에서 교체됐다 / 29세 / 안 쓰는 말: 무조건, 당연히",
            generationExecuteRepository.findById(second.id).orElseThrow().existingCharactersNote,
        )

        // 빈 목록은 "알려주지 않는다"는 지정이다
        val third = generationService.requestGeneration(
            generationRequest(ref.id).copy(existingCharacterIds = emptyList()),
        )
        executor.execute(third.id)
        assertFalse(fakeLlmClient.lastPrompt.inputs.single().inputText.contains("서윤 / 웹툰"))
        assertEquals(null, generationExecuteRepository.findById(third.id).orElseThrow().existingCharactersNote)
    }

    @Test
    fun `1단계와 5단계를 세 축 이상 지정하면 실행을 생성하지 않는다`() {
        val ref = existingCharacterPromptRef("L0 extreme trait test")
        val extremeRequest = generationRequest(ref.id).copy(
            opennessStageId = traitStageId(BigFiveTrait.OPENNESS, 1),
            conscientiousnessStageId = traitStageId(BigFiveTrait.CONSCIENTIOUSNESS, 5),
            extraversionStageId = traitStageId(BigFiveTrait.EXTRAVERSION, 1),
        )

        val failure = assertFailsWith<InvalidRequestException> {
            generationService.requestGeneration(extremeRequest)
        }

        assertTrue(failure.detail.contains("최대 2개 축"), failure.detail)
        assertFalse(generationExecuteRepository.findAll().any { it.promptTemplateRef.id == ref.id })
    }

    private fun existingCharacterPromptRef(templateName: String): PromptTemplateRef {
        val template = promptTemplateRepository.save(
            PromptTemplate(name = templateName, type = PromptTemplateType.CHARACTER_GENERATION),
        )
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system",
        ).also { it.addInput("user", "{{EXISTING_CHARACTERS}}") }
        return promptTemplateRefRepository.saveAndFlush(ref)
    }

    private fun traitStageId(trait: BigFiveTrait, stage: Int): Long =
        assertNotNull(personalityTraitStageRepository.findByTraitAndStage(trait, stage)).id

    @Test
    fun `LLM 호출 실패를 FAILED 상태와 추적 정보로 저장한다`() {
        val template = promptTemplateRepository.save(PromptTemplate(name = "L0 failure test", type = PromptTemplateType.CHARACTER_GENERATION))
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system",
        ).also { it.addInput("user", "{{DOMAIN}}") }
        promptTemplateRefRepository.saveAndFlush(ref)
        fakeLlmClient.failure = IllegalStateException("OpenAI unavailable")

        val generation = generationService.requestGeneration(generationRequest(ref.id))
        executor.execute(generation.id)

        val failed = generationExecuteRepository.findById(generation.id).orElseThrow()
        assertEquals(L0GenerationExecuteStatus.FAILED, failed.status)
        assertEquals("OpenAI unavailable", failed.message)
        assertTrue(assertNotNull(failed.errorTrace).contains("IllegalStateException"))
        assertNotNull(failed.elapsed)
        assertEquals(null, failed.completedAt)
        assertEquals(null, failed.character)
        assertFalse(characterRepository.existsByGenerationExecuteId(generation.id))
    }

    @Test
    fun `Big Five 단계 ID가 요청 축과 다르면 실행을 생성하지 않는다`() {
        val template = promptTemplateRepository.save(PromptTemplate(name = "L0 invalid option test", type = PromptTemplateType.CHARACTER_GENERATION))
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system",
        ).also { it.addInput("user", "{{O_DESC}}") }
        promptTemplateRefRepository.saveAndFlush(ref)
        val validRequest = generationRequest(ref.id)

        val failure = assertFailsWith<InvalidRequestException> {
            generationService.requestGeneration(
                validRequest.copy(opennessStageId = validRequest.conscientiousnessStageId),
            )
        }

        assertTrue(failure.detail.contains("OPENNESS"))
        assertFalse(generationExecuteRepository.findAll().any { it.promptTemplateRef.id == ref.id })
    }

    @Test
    fun `필수 축만 입력하면 나머지는 null로 저장하고 성향은 3단계 기본값으로 채운다`() {
        val template = promptTemplateRepository.save(PromptTemplate(name = "L0 nullable input test", type = PromptTemplateType.CHARACTER_GENERATION))
        val placeholders = listOf(
            "DESCRIPTION",
            "NAME",
            "DOMAIN",
            "LIE_TYPE",
            "LIE_STATEMENT",
            "WOUND_TIMING",
            "WOUND_PLACE",
            "WOUND_AGENT",
            "WOUND_FORM",
            "WOUND_DURATION",
            "O_STAGE",
            "O_DESC",
            "C_STAGE",
            "C_DESC",
            "E_STAGE",
            "E_DESC",
            "A_STAGE",
            "A_DESC",
            "N_STAGE",
            "N_DESC",
            "ATTACHMENT",
            "ATTACHMENT_DESC",
            "EMOTIONAL_EXPRESSION",
            "EMOTIONAL_DESC",
            "AVOIDANCE_STYLE",
            "AVOIDANCE_DESC",
            "SPEECH_ARCHETYPE",
            "SPEECH_DESC",
            "AGE",
            "GENDER",
            "SPEECH_REGISTER",
            "SCALE",
            "EMOTION",
            "EXTRA_CONSTRAINTS",
        )
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = placeholders.joinToString("|") { "{{$it}}" },
        ).also { it.addInput("user", "{{DOMAIN}}/{{AGE}}/{{GENDER}}") }
        promptTemplateRefRepository.saveAndFlush(ref)
        fakeLlmClient.failure = null
        fakeLlmClient.response = objectMapper.readValue(RESPONSE_JSON, L0CharacterResponse::class.java)

        val options = testOptions()
        val generation = generationService.requestGeneration(
            L0CharacterGenerationRequest(
                refId = ref.id,
                attachmentStyleId = options.attachmentStyleId,
                emotionalExpressionId = options.emotionalExpressionId,
                avoidanceStyleId = options.avoidanceStyleId,
                speechArchetypeId = options.speechArchetypeId,
            ),
        )
        val saved = generationExecuteRepository.findOneWithDetailsById(generation.id)!!
        assertEquals(null, saved.domain)
        assertEquals(null, saved.lie)
        assertEquals(null, saved.name)
        assertEquals(null, saved.woundTiming)
        assertEquals(null, saved.woundPlace)
        assertEquals(null, saved.woundAgent)
        assertEquals(null, saved.woundForm)
        assertEquals(null, saved.woundDuration)
        // 성향은 지정하지 않아도 v4 운영 기본값인 3단계로 채워진다
        assertEquals(3, saved.opennessStage?.stage)
        assertEquals(3, saved.neuroticismStage?.stage)
        assertEquals(options.attachmentStyleId, saved.attachmentStyle?.id)
        assertEquals(null, saved.description)
        assertEquals(null, saved.age)
        assertEquals(null, saved.gender)
        assertEquals(null, saved.speechRegister)
        assertEquals(null, saved.scale)
        assertEquals(null, saved.emotion)
        assertEquals(null, saved.extraConstraints)

        executor.execute(generation.id)

        // 성향과 표현 축은 값이 채워지고, 지정하지 않은 나머지 자리만 빈 문자열로 남는다
        val renderedInstructions = fakeLlmClient.lastPrompt.instructions
        assertTrue(renderedInstructions.contains("3|이 축은 이 인물을 설명하는 특징이 아니다"), renderedInstructions)
        assertFalse(renderedInstructions.contains("{{"))
        assertEquals("//", fakeLlmClient.lastPrompt.inputs.single().inputText)
        val detail = generationService.getGeneration(generation.id)
        assertEquals(null, detail.domainId)
        assertEquals(null, detail.lieId)
        assertEquals(null, detail.name)
        assertEquals(null, detail.woundTimingId)
        assertEquals(null, detail.woundPlaceId)
        assertEquals(null, detail.woundAgentId)
        assertEquals(null, detail.woundFormId)
        assertEquals(null, detail.woundDurationId)
        assertEquals(null, detail.age)
        assertEquals(null, detail.gender)
        assertEquals(null, detail.speechRegisterId)
        assertEquals(null, detail.scale)
        assertEquals(null, detail.emotion)
    }

    private fun generationRequest(refId: Long): L0CharacterGenerationRequest {
        val options = testOptions()
        return L0CharacterGenerationRequest(
            refId = refId,
            name = "서윤",
            domainId = options.domainId,
            lieId = options.lieId,
            woundTimingId = woundTimingRepository.findAllByOrderByIdAsc().first().id,
            woundPlaceId = woundPlaceRepository.findAllByOrderByIdAsc().first().id,
            woundAgentId = woundAgentRepository.findAllByOrderByIdAsc().first().id,
            woundFormId = woundFormRepository.findAllByOrderByIdAsc().first().id,
            woundDurationId = woundDurationRepository.findAllByOrderByIdAsc().first().id,
            opennessStageId = options.opennessStageId,
            conscientiousnessStageId = options.conscientiousnessStageId,
            extraversionStageId = options.extraversionStageId,
            agreeablenessStageId = options.agreeablenessStageId,
            neuroticismStageId = options.neuroticismStageId,
            attachmentStyleId = options.attachmentStyleId,
            emotionalExpressionId = options.emotionalExpressionId,
            avoidanceStyleId = options.avoidanceStyleId,
            speechArchetypeId = options.speechArchetypeId,
            age = 29,
            gender = Gender.FEMALE,
            speechRegisterId = options.speechRegisterId,
            scale = 2,
            emotion = 4,
        )
    }

    private fun testOptions() = l0GenerationTestOptions(
        domainRepository,
        lieRepository,
        personalityTraitStageRepository,
        attachmentStyleRepository,
        emotionalExpressionRepository,
        avoidanceStyleRepository,
        speechArchetypeRepository,
        speechRegisterRepository,
    )

    @TestConfiguration
    class FakeLlmConfiguration {
        @Bean
        @Primary
        fun fakeLlmClient(): FakeL0CharacterLlmClient = FakeL0CharacterLlmClient()

        @Bean
        @Primary
        fun fakeTextEmbeddingClient(): TextEmbeddingClient = FakeTextEmbeddingClient()
    }

    class FakeL0CharacterLlmClient : L0CharacterLlmClient {
        lateinit var response: L0CharacterResponse
        lateinit var lastPrompt: GenerationPrompt
        var failure: RuntimeException? = null

        override fun generate(prompt: GenerationPrompt): L0CharacterLlmResult {
            lastPrompt = prompt
            failure?.let { throw it }
            return L0CharacterLlmResult(character = response, rawJson = RESPONSE_JSON)
        }
    }

    private companion object {
        val RESPONSE_JSON =
            """
            {
              "identity":{"name":"서윤","age":29,"gender":"여성","occupation":"웹툰 작가","daily_life":"작업한다","one_line":"선을 지킨다","concrete_details":["왼손잡이","밤에 작업","마감 습관"]},
              "core":{"lie":{"type":"대체 가능성 공포형","statement":"나는 대체된다","behavioral_rule":["칭찬을 받으면 다음 작업 이야기로 넘긴다","누가 자기 몫을 대신하려 하면 먼저 손을 뺀다"],"wound":"첫 연재에서 교체됐다"},"want":"자기 방식의 작품을 남긴다","need":"교체와 가치가 무관함을 안다","want_need_conflict":"인정을 좇을수록 자기 방식을 버린다","contradiction":{"trait_a":"먼저 선을 긋는다","trait_b":"떠나면 붙잡는다","shared_root":"대체될 두려움"},"non_negotiables":[{"domain":"관계를 다루는 방식","statement":"비교하지 않는다","tie_to_lie":"교체 공포","user_facing":true},{"domain":"세계를 보는 눈","statement":"결과를 과장하지 않는다","tie_to_lie":"교체 공포","user_facing":false},{"domain":"일을 대하는 기준","statement":"남의 선을 베끼지 않는다","tie_to_lie":"교체 공포","user_facing":false}],"forbidden_behaviors":[{"behavior":"상대를 시험하지 않는다","tie_to_lie":"교체 공포"},{"behavior":"성과로 애정을 사지 않는다","tie_to_lie":"교체 공포"}]},
              "relationship_stages":{"early":{"attachment":"먼저 묻지 않는다","address":"이름 없이 존댓말로 부른다","emotional_range":"불편함만 짧게 드러낸다","avoidance":"과거를 물으면 한 문장으로 끊는다"},"deepening":{"attachment":"작업 이야기만 연다","address":"이름을 부르되 존댓말을 유지한다","emotional_range":"작업에 대한 감정까지 말한다","avoidance":"화제를 돌리되 이유를 한 줄 붙인다"},"close":{"attachment":"거리를 유지한 채 곁에 둔다","address":"이름을 줄여 부르고 반말을 섞는다","emotional_range":"가라앉은 상태도 숨기지 않는다","avoidance":"말하지 않겠다고 분명히 말한다"}},
              "relationship_events":{"on_rejection":"한 번 물러난 뒤 다시 묻지 않는다","on_conflict":"근거를 한 줄로 말하고 물러서지 않는다","on_absence":"없었던 일처럼 작업 이야기부터 잇는다","on_over_approach":"속도를 늦추고 하던 이야기로 돌아온다"},
              "behavior":{"emotional_expression_note":"감정을 사실로 바꾼다","avoidance_note":"한 문장 뒤 침묵한다"},
              "voice":{"speech_archetype_note":"작화 도구에 빗댄다","sentence_pattern":"짧은 결론 뒤 비유","verbal_tics":[{"situation":"거절할 때","description":"선을 다시 긋는다고 말한다"}],"banned_words":{"from_lie":["무조건"],"from_background":["당연히"]},"under_stress":"문장이 더 짧아진다"},
              "relations":[{"slot":"과거","name":"민호","relation":"전 편집자","description":"첫 연재에서 교체를 알렸다","presence":"부재","tie_to_lie":"대체 공포의 기원"}],
              "disclosure_ladder":{"surface":["마감 습관"],"mid":["편집자 이야기"],"deep":["교체 경험"],"deepest":["자기 가치에 대한 공포"]},
              "voice_samples":[{"situation":"의견에 반대했을 때","line":"그 선은 제가 정할게요.","demonstrates":"거리 유지"}],
              "design_note":{"recognizability":"작화 도구 비유","long_term_viability":"여러 관계에서 교체 공포가 반복된다","imbalance_note":"","trait_reflection":"낯선 방식에서 행동을 만들지 않는다"}
            }
            """.trimIndent()
    }
}
