package ltmp.charactergen.api

import ltmp.charactergen.config.L0ReferenceDataInitializer
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_BRANCH_NAME
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INPUT_TEXT
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INSTRUCTIONS
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_TEMPLATE_NAME
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.repo.AttachmentStyleRepository
import ltmp.charactergen.data.repo.AvoidanceStyleRepository
import ltmp.charactergen.data.repo.DomainRepository
import ltmp.charactergen.data.repo.EmotionalExpressionRepository
import ltmp.charactergen.data.repo.LieRepository
import ltmp.charactergen.data.repo.PersonalityTraitStageRepository
import ltmp.charactergen.data.repo.PromptTemplateRefRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import ltmp.charactergen.data.repo.SpeechArchetypeRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
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
    @Autowired private val promptTemplateRepository: PromptTemplateRepository,
    @Autowired private val promptTemplateRefRepository: PromptTemplateRefRepository,
) {
    @Test
    fun `OpenAPI 문서에 ref 관리와 페이징 스펙을 노출한다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs'].get.parameters[?(@.name == 'sort')]") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/branches'].post") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/versions'].post") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}'].put") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}'].delete") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}'].get") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}'].delete") { exists() }
            jsonPath("$.paths['/api/v1/prompt-template-refs']") { doesNotExist() }
            jsonPath("$.paths['/api/v1/prompt-template-versions']") { doesNotExist() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].post") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].post.responses['429']") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'sort')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations/{generationId}'].get") { exists() }
        }
    }

    @Test
    fun `BRANCH를 편집하고 VERSION을 발행하며 참조 무결성을 지킨다`() {
        val templateId = createTemplate("L0 Character")
        val otherTemplateId = createTemplate("Other Character")

        val emptyBranch = mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/branches", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"main","fromRefId":null}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.type") { value("BRANCH") }
            jsonPath("$.name") { value("main") }
            jsonPath("$.version") { doesNotExist() }
            jsonPath("$.instructions") { value("") }
            jsonPath("$.inputs.length()") { value(0) }
            jsonPath("$.revision") { value(0) }
        }.andReturn()
        val branchId = responseId(emptyBranch.response.contentAsString)
        assertEquals("http://localhost/api/v1/prompt-templates/$templateId/refs/main", emptyBranch.response.getHeader("Location"))

        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$branchId}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }

        val updateBody = """
            {
              "name":"main",
              "instructions":"Generate character",
              "inputs":[
                {"role":"user","inputText":"Create an L0 character"},
                {"role":"assistant","inputText":"Return JSON only"}
              ],
              "revision":0
            }
        """.trimIndent()
        val updatedBranch = mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = updateBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.instructions") { value("Generate character") }
            jsonPath("$.inputs[0].role") { value("user") }
            jsonPath("$.inputs[1].role") { value("assistant") }
            jsonPath("$.revision") { value(1) }
        }.andReturn()

        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = updateBody
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("RESOURCE_CONFLICT") }
        }

        val versionOne = mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$branchId}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.type") { value("VERSION") }
            jsonPath("$.name") { doesNotExist() }
            jsonPath("$.version") { value(1) }
            jsonPath("$.fromRefId") { value(branchId) }
            jsonPath("$.fromRefRevision") { value(1) }
            jsonPath("$.inputs[0].inputText") { value("Create an L0 character") }
        }.andReturn()
        val versionOneId = responseId(versionOne.response.contentAsString)
        assertEquals(
            "http://localhost/api/v1/prompt-templates/$templateId/refs/versions/1",
            versionOne.response.getHeader("Location"),
        )

        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$versionOneId}"""
        }.andExpect { status { isBadRequest() } }

        val copiedBranch = mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/branches", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"experiment","fromRefId":$versionOneId}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.fromRefId") { value(versionOneId) }
            jsonPath("$.instructions") { value("Generate character") }
            jsonPath("$.inputs[1].inputText") { value("Return JSON only") }
        }.andReturn()
        val copiedBranchId = responseId(copiedBranch.response.contentAsString)

        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/branches", otherTemplateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"cross-copy","fromRefId":$branchId}"""
        }.andExpect { status { isBadRequest() } }

        mockMvc.get("/api/v1/prompt-templates/{promptTemplateId}/refs", templateId) {
            param("type", "BRANCH")
            param("name", "experiment")
            param("page", "0")
            param("size", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(copiedBranchId) }
            jsonPath("$.page.totalElements") { value(1) }
        }

        mockMvc.get("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "experiment").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(copiedBranchId) }
            jsonPath("$.name") { value("experiment") }
        }
        mockMvc.get("/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}", templateId, 1).andExpect {
            status { isOk() }
            jsonPath("$.id") { value(versionOneId) }
            jsonPath("$.version") { value(1) }
        }

        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}", templateId, 1).andExpect {
            status { isConflict() }
        }
        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main").andExpect {
            status { isConflict() }
        }

        createBranch(templateId, "deletable")
        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "deletable").andExpect {
            status { isNoContent() }
        }
        mockMvc.delete("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isConflict() }
        }

        assertNotNull(updatedBranch.response.contentAsString)
    }

    @Test
    fun `BRANCH 실행에는 원본 스냅샷을 저장하고 VERSION 실행에는 저장하지 않는다`() {
        val templateId = createTemplate("Generation Template")
        val branchId = createBranch(templateId, "main")
        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"name":"main","instructions":"System instruction","inputs":[{"role":"user","inputText":"Hello {{DOMAIN}}"}],"revision":0}
            """.trimIndent()
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = generationRequest(branchId)
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.refId") { value(branchId) }
            jsonPath("$.refType") { value("BRANCH") }
            jsonPath("$.status") { value("PREPARING") }
            jsonPath("$.promptRefSnapshotJson") { value(org.hamcrest.Matchers.containsString("System instruction")) }
            jsonPath("$.promptRefSnapshotJson") { value(org.hamcrest.Matchers.containsString("Hello {{DOMAIN}}")) }
        }

        val versionResponse = mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$branchId}"""
        }.andExpect { status { isCreated() } }.andReturn()
        val versionId = responseId(versionResponse.response.contentAsString)
        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = generationRequest(versionId)
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.refType") { value("VERSION") }
            jsonPath("$.promptRefSnapshotJson") { doesNotExist() }
        }
        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}", templateId, 1).andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `생성 실행 이력을 페이징 검색하고 단일 상세 조회한다`() {
        val templateId = createTemplate("Generation History Template")
        val branchId = createBranch(templateId, "main")
        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"main","instructions":"instruction","inputs":[{"role":"user","inputText":"input"}],"revision":0}"""
        }.andExpect { status { isOk() } }

        val firstGenerationId = generationId(
            mockMvc.post("/api/v1/l0-characters/generations") {
                contentType = MediaType.APPLICATION_JSON
                content = generationRequest(branchId)
            }.andExpect { status { isAccepted() } }.andReturn().response.contentAsString,
        )
        val secondGenerationId = generationId(
            mockMvc.post("/api/v1/l0-characters/generations") {
                contentType = MediaType.APPLICATION_JSON
                content = generationRequest(branchId)
            }.andExpect { status { isAccepted() } }.andReturn().response.contentAsString,
        )

        mockMvc.get("/api/v1/l0-characters/generations") {
            param("refId", branchId.toString())
            param("status", "PREPARING")
            param("page", "0")
            param("size", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(secondGenerationId) }
            jsonPath("$.content[0].refId") { value(branchId) }
            jsonPath("$.content[0].status") { value("PREPARING") }
            jsonPath("$.page.size") { value(1) }
            jsonPath("$.page.totalElements") { value(2) }
        }

        mockMvc.get("/api/v1/l0-characters/generations/{generationId}", firstGenerationId).andExpect {
            status { isOk() }
            jsonPath("$.id") { value(firstGenerationId) }
            jsonPath("$.refId") { value(branchId) }
            jsonPath("$.refType") { value("BRANCH") }
            jsonPath("$.userInputJson") { value(org.hamcrest.Matchers.containsString("\"refId\":$branchId")) }
        }
        mockMvc.get("/api/v1/l0-characters/generations/{generationId}", Long.MAX_VALUE).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
        }
    }

    @Test
    fun `삭제한 VERSION 번호를 재사용하지 않는다`() {
        val templateId = createTemplate("Version Sequence Template")
        val branchId = createBranch(templateId, "main")
        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"main","instructions":"instruction","inputs":[{"role":"user","inputText":"input"}],"revision":0}"""
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$branchId}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.version") { value(1) }
        }
        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/versions/{version}", templateId, 1).andExpect {
            status { isNoContent() }
        }
        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/versions", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"fromRefId":$branchId}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.version") { value(2) }
        }
        mockMvc.get("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isOk() }
            jsonPath("$.lastVersion") { value(2) }
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

    @Test
    fun `L0 캐릭터 프롬프트 BRANCH와 복사된 VERSION을 초기화한다`() {
        referenceDataInitializer.run(DefaultApplicationArguments())

        val template = assertNotNull(promptTemplateRepository.findByName(L0_CHARACTER_PROMPT_TEMPLATE_NAME))
        assertEquals(1, template.lastVersion)

        val refs = promptTemplateRefRepository.findAll().filter { it.promptTemplate.id == template.id }
        assertEquals(2, refs.size)
        val branch = refs.single { it.type == PromptTemplateRefType.BRANCH }
        val version = refs.single { it.type == PromptTemplateRefType.VERSION }

        assertEquals(L0_CHARACTER_PROMPT_BRANCH_NAME, branch.name)
        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS, branch.instructions)
        assertEquals(1, branch.inputs.size)
        assertEquals("user", branch.inputs.single().role)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT, branch.inputs.single().inputText)

        assertEquals(1, version.version)
        assertEquals(branch.id, version.fromRef?.id)
        assertEquals(branch.revision, version.fromRefRevision)
        assertEquals(branch.instructions, version.instructions)
        assertEquals(branch.inputs.single().role, version.inputs.single().role)
        assertEquals(branch.inputs.single().inputText, version.inputs.single().inputText)
    }

    private fun createTemplate(name: String): Long = locationId(
        mockMvc.post("/api/v1/prompt-templates") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.getHeader("Location"),
    )

    private fun createBranch(templateId: Long, name: String): Long = responseId(
        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/branches", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString,
    )

    private fun locationId(location: String?): Long = assertNotNull(location).substringAfterLast('/').toLong()

    private fun responseId(responseBody: String): Long =
        assertNotNull(Regex(""""id":(\d+)""").find(responseBody)).groupValues[1].toLong()

    private fun generationId(responseBody: String): Long =
        assertNotNull(Regex(""""id":(\d+)""").find(responseBody)).groupValues[1].toLong()

    private fun generationRequest(refId: Long) = """
        {
          "refId":$refId,
          "domain":"웹툰",
          "lieType":"대체 가능성 공포형",
          "opennessStage":3,
          "conscientiousnessStage":3,
          "extraversionStage":3,
          "agreeablenessStage":3,
          "neuroticismStage":3,
          "attachment":"일정 거리 유지",
          "emotionalExpression":"사실 치환",
          "avoidanceStyle":"짧게 답하고 침묵",
          "speechArchetype":"비유형",
          "age":29,
          "gender":"여성",
          "speechRegister":"기본적으로 존댓말",
          "scale":2,
          "emotion":4
        }
    """.trimIndent()
}
