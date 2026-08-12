package ltmp.charactergen.api

import ltmp.charactergen.config.L0ReferenceDataInitializer
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagementApiIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val referenceDataInitializer: L0ReferenceDataInitializer,
    @Autowired private val domainRepository: DomainRepository,
    @Autowired private val lieRepository: LieRepository,
    @Autowired private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    @Autowired private val attachmentStyleRepository: AttachmentStyleRepository,
    @Autowired private val emotionalExpressionRepository: EmotionalExpressionRepository,
    @Autowired private val avoidanceStyleRepository: AvoidanceStyleRepository,
    @Autowired private val speechArchetypeRepository: SpeechArchetypeRepository,
) {
    @Test
    fun `OpenAPI 문서에 관리 API 스펙을 노출한다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get.parameters[?(@.name == 'sort')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates'].post") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{id}'].put") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{id}'].delete") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].get.parameters[?(@.name == 'sort')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].post") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions/{id}'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions/{id}'].put") { doesNotExist() }
            jsonPath("$.paths['/api/v1/prompt-template-versions/{id}'].delete") { doesNotExist() }
            jsonPath("$.paths['/api/v1/l0-characters/generation-options'].get") { exists() }
        }
    }

    @Test
    fun `프롬프트 템플릿과 버전을 생성 검색 조회 수정 삭제한다`() {
        val createTemplateResult = mockMvc.post("/api/v1/prompt-templates") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"L0 Character"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("L0 Character") }
        }.andReturn()

        val templateLocation = assertNotNull(createTemplateResult.response.getHeader("Location"))
        val templateId = templateLocation.substringAfterLast('/').toLong()

        mockMvc.get("/api/v1/prompt-templates") {
            param("name", "character")
            param("page", "0")
            param("size", "1")
            param("sort", "name,asc")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(templateId) }
            jsonPath("$.page.size") { value(1) }
            jsonPath("$.page.totalElements") { value(1) }
        }

        mockMvc.put("/api/v1/prompt-templates/{id}", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"L0 Character Updated"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("L0 Character Updated") }
        }

        val createVersionResult = mockMvc.post("/api/v1/prompt-template-versions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"promptTemplateId":$templateId,"name":"v1","prompt":"Generate character"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.promptTemplateId") { value(templateId) }
            jsonPath("$.promptTemplateName") { value("L0 Character Updated") }
            jsonPath("$.prompt") { value("Generate character") }
        }.andReturn()

        val versionLocation = assertNotNull(createVersionResult.response.getHeader("Location"))
        val versionId = versionLocation.substringAfterLast('/').toLong()

        mockMvc.get("/api/v1/prompt-template-versions/{id}", versionId).andExpect {
            status { isOk() }
            jsonPath("$.name") { value("v1") }
        }
        mockMvc.get("/api/v1/prompt-template-versions") {
            param("promptTemplateId", templateId.toString())
            param("name", "v")
            param("page", "0")
            param("size", "1")
            param("sort", "id,desc")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(versionId) }
            jsonPath("$.page.size") { value(1) }
            jsonPath("$.page.totalElements") { value(1) }
        }
        mockMvc.delete("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("RESOURCE_CONFLICT") }
        }

        val deletableResult = mockMvc.post("/api/v1/prompt-templates") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Deletable"}"""
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        val deletableId = assertNotNull(deletableResult.response.getHeader("Location")).substringAfterLast('/').toLong()

        mockMvc.delete("/api/v1/prompt-templates/{id}", deletableId).andExpect {
            status { isNoContent() }
        }
        mockMvc.get("/api/v1/prompt-templates/{id}", deletableId).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
        }
    }

    @Test
    fun `캐릭터 생성에 필요한 모든 축 정보를 한 번에 조회한다`() {
        referenceDataInitializer.run(DefaultApplicationArguments())

        assertEquals(2, domainRepository.count())
        assertEquals(12, lieRepository.count())
        assertEquals(25, personalityTraitStageRepository.count())
        assertEquals(6, attachmentStyleRepository.count())
        assertEquals(6, emotionalExpressionRepository.count())
        assertEquals(6, avoidanceStyleRepository.count())
        assertEquals(8, speechArchetypeRepository.count())

        val opennessStageThree = personalityTraitStageRepository.findAll().single {
            it.trait == BigFiveTrait.OPENNESS && it.stage == 3
        }
        assertEquals(
            "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라.",
            opennessStageThree.description,
        )

        mockMvc.get("/api/v1/l0-characters/generation-options").andExpect {
            status { isOk() }
            jsonPath("$.domains.length()") { value(2) }
            jsonPath("$.lies.length()") { value(12) }
            jsonPath("$.personalityTraitStages.length()") { value(25) }
            jsonPath("$.attachments.length()") { value(6) }
            jsonPath("$.emotionalExpressions.length()") { value(6) }
            jsonPath("$.avoidanceStyles.length()") { value(6) }
            jsonPath("$.speechArchetypes.length()") { value(8) }
        }
    }
}
