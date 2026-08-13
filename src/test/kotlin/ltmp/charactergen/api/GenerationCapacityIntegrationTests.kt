package ltmp.charactergen.api

import ltmp.charactergen.client.openai.L0CharacterLlmClient
import ltmp.charactergen.client.openai.L0CharacterLlmResult
import ltmp.charactergen.l0GenerationTestOptions
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import ltmp.charactergen.data.repo.L0GenerationExecuteRepository
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import ltmp.charactergen.data.repo.SpeechRegisterRepository
import ltmp.charactergen.service.generation.GenerationPrompt
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(properties = ["openai.generation.max-concurrency=1"])
@AutoConfigureMockMvc
@Import(GenerationCapacityIntegrationTests.BlockingLlmConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GenerationCapacityIntegrationTests(
    private val mockMvc: MockMvc,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val promptTemplateRefRepository: PromptTemplateRefRepository,
    private val generationExecuteRepository: L0GenerationExecuteRepository,
    private val domainRepository: DomainRepository,
    private val lieRepository: LieRepository,
    private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    private val attachmentStyleRepository: AttachmentStyleRepository,
    private val emotionalExpressionRepository: EmotionalExpressionRepository,
    private val avoidanceStyleRepository: AvoidanceStyleRepository,
    private val speechArchetypeRepository: SpeechArchetypeRepository,
    private val speechRegisterRepository: SpeechRegisterRepository,
    private val blockingLlmClient: BlockingL0CharacterLlmClient,
) {
    @Test
    fun `동시 실행 한도를 초과하면 429를 반환하고 execute 레코드를 만들지 않는다`() {
        val template = promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "capacity-${UUID.randomUUID()}"),
        )
        val ref = PromptTemplateRef(
            promptTemplate = template,
            type = PromptTemplateRefType.BRANCH,
            name = "main",
            instructions = "system instruction",
        ).also { it.addInput("user", "generate {{DOMAIN}}") }
        promptTemplateRefRepository.saveAndFlush(ref)

        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = generationRequest(ref.id)
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.status") { value("PREPARING") }
        }
        assertTrue(blockingLlmClient.started.await(5, TimeUnit.SECONDS))
        assertEquals(1, generationExecuteRepository.count())

        try {
            mockMvc.post("/api/v1/l0-characters/generations") {
                contentType = MediaType.APPLICATION_JSON
                content = generationRequest(ref.id)
            }.andExpect {
                status { isTooManyRequests() }
                jsonPath("$.code") { value("GENERATION_CAPACITY_EXCEEDED") }
            }
            assertEquals(1, generationExecuteRepository.count())
        } finally {
            blockingLlmClient.release.countDown()
        }
        assertTrue(blockingLlmClient.returned.await(5, TimeUnit.SECONDS))
    }

    private fun generationRequest(refId: Long): String {
        val options = l0GenerationTestOptions(
            domainRepository,
            lieRepository,
            personalityTraitStageRepository,
            attachmentStyleRepository,
            emotionalExpressionRepository,
            avoidanceStyleRepository,
            speechArchetypeRepository,
            speechRegisterRepository,
        )
        return """
        {
          "refId":$refId,
          "domainId":${options.domainId},
          "lieId":${options.lieId},
          "opennessStageId":${options.opennessStageId},
          "conscientiousnessStageId":${options.conscientiousnessStageId},
          "extraversionStageId":${options.extraversionStageId},
          "agreeablenessStageId":${options.agreeablenessStageId},
          "neuroticismStageId":${options.neuroticismStageId},
          "attachmentStyleId":${options.attachmentStyleId},
          "emotionalExpressionId":${options.emotionalExpressionId},
          "avoidanceStyleId":${options.avoidanceStyleId},
          "speechArchetypeId":${options.speechArchetypeId},
          "speechRegisterId":${options.speechRegisterId},
          "age":29,
          "gender":"FEMALE",
          "scale":2,
          "emotion":4
        }
        """.trimIndent()
    }

    @TestConfiguration
    class BlockingLlmConfiguration {
        @Bean
        @Primary
        fun blockingLlmClient(): BlockingL0CharacterLlmClient = BlockingL0CharacterLlmClient()
    }

    class BlockingL0CharacterLlmClient : L0CharacterLlmClient {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val returned = CountDownLatch(1)

        override fun generate(prompt: GenerationPrompt): L0CharacterLlmResult {
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS)) { "Test did not release the blocking LLM client" }
            returned.countDown()
            throw IllegalStateException("Expected test failure after release")
        }
    }
}
