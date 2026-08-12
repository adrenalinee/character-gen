package ltmp.charactergen.api

import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.entity.AttachmentStyleEntity
import ltmp.charactergen.data.entity.AvoidanceStyleEntity
import ltmp.charactergen.data.entity.DomainEntity
import ltmp.charactergen.data.entity.EmotionalExpressionEntity
import ltmp.charactergen.data.entity.LieEntity
import ltmp.charactergen.data.entity.PersonalityTraitStageEntity
import ltmp.charactergen.data.entity.SpeechArchetypeEntity
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagementApiIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
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
            jsonPath("$.paths['/api/v1/prompt-templates'].post") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{id}'].put") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{id}'].delete") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-versions'].get") { exists() }
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
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(templateId) }
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
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(versionId) }
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
        domainRepository.save(DomainEntity(name = "웹툰", description = "웹툰 분야"))
        lieRepository.save(LieEntity(type = "대체 가능성 공포형", coreStatement = "나는 언제든 대체될 수 있다"))
        personalityTraitStageRepository.save(
            PersonalityTraitStageEntity(trait = BigFiveTrait.OPENNESS, stage = 3, description = "균형 잡힌 개방성"),
        )
        attachmentStyleRepository.save(AttachmentStyleEntity(type = "일정 거리 유지", description = "관계에서 거리를 둔다"))
        emotionalExpressionRepository.save(EmotionalExpressionEntity(type = "사실 치환", description = "감정을 사실로 표현한다"))
        avoidanceStyleRepository.save(AvoidanceStyleEntity(type = "침묵", description = "대답을 멈춘다"))
        speechArchetypeRepository.save(SpeechArchetypeEntity(name = "비유형", description = "비유를 사용한다"))

        mockMvc.get("/api/v1/l0-characters/generation-options").andExpect {
            status { isOk() }
            jsonPath("$.domains[0].name") { value("웹툰") }
            jsonPath("$.lies[0].type") { value("대체 가능성 공포형") }
            jsonPath("$.personalityTraitStages[0].trait") { value("OPENNESS") }
            jsonPath("$.personalityTraitStages[0].stage") { value(3) }
            jsonPath("$.attachments[0].type") { value("일정 거리 유지") }
            jsonPath("$.emotionalExpressions[0].type") { value("사실 치환") }
            jsonPath("$.avoidanceStyles[0].type") { value("침묵") }
            jsonPath("$.speechArchetypes[0].name") { value("비유형") }
        }
    }
}
