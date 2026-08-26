package ltmp.charactergen.api

import ltmp.charactergen.config.L0ReferenceDataInitializer
import ltmp.charactergen.l0GenerationTestOptions
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_BRANCH_NAME
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INPUT_TEXT
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INSTRUCTIONS
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INPUT_TEXT_V3_1
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_1
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INPUT_TEXT_V3_2
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_2
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INPUT_TEXT_V4
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_INSTRUCTIONS_V4
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_TEMPLATE_NAME
import ltmp.charactergen.data.BigFiveTrait
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.PromptTemplateType
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
import ltmp.charactergen.data.repo.WoundAgentRepository
import ltmp.charactergen.data.repo.WoundDurationRepository
import ltmp.charactergen.data.repo.WoundFormRepository
import ltmp.charactergen.data.repo.WoundPlaceRepository
import ltmp.charactergen.data.repo.WoundTimingRepository
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
    @Autowired private val woundTimingRepository: WoundTimingRepository,
    @Autowired private val woundPlaceRepository: WoundPlaceRepository,
    @Autowired private val woundAgentRepository: WoundAgentRepository,
    @Autowired private val woundFormRepository: WoundFormRepository,
    @Autowired private val woundDurationRepository: WoundDurationRepository,
    @Autowired private val personalityTraitStageRepository: PersonalityTraitStageRepository,
    @Autowired private val attachmentStyleRepository: AttachmentStyleRepository,
    @Autowired private val emotionalExpressionRepository: EmotionalExpressionRepository,
    @Autowired private val avoidanceStyleRepository: AvoidanceStyleRepository,
    @Autowired private val speechArchetypeRepository: SpeechArchetypeRepository,
    @Autowired private val speechRegisterRepository: SpeechRegisterRepository,
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
            jsonPath("$.components.schemas.PromptTemplateSimpleDto") { exists() }
            jsonPath("$.components.schemas.PromptTemplateDto.properties.defaultBranch") { exists() }
            jsonPath("$.components.schemas.PromptTemplateSimpleDto.properties.defaultBranch") { exists() }
            jsonPath("$.components.schemas.CreatePromptTemplateBranchRequest.properties.name.pattern") {
                value("^[A-Za-z0-9_-]+$")
            }
            jsonPath("$.components.schemas.PromptTemplateRefSimpleDto") { exists() }
            jsonPath("$.components.schemas.L0GenerationExecuteSimpleDto") { exists() }
            jsonPath("$.components.schemas.PromptTemplateRefSimpleDto.properties.instructions") { doesNotExist() }
            jsonPath("$.components.schemas.PromptTemplateRefSimpleDto.properties.inputs") { doesNotExist() }
            jsonPath("$.components.schemas.L0GenerationExecuteSimpleDto.properties.promptRefSnapshotJson") { doesNotExist() }
            jsonPath("$.components.schemas.L0GenerationExecuteSimpleDto.properties.userInputJson") { doesNotExist() }
            jsonPath("$.components.schemas.L0GenerationExecuteSimpleDto.properties.llmOutput") { doesNotExist() }
            jsonPath("$.components.schemas.L0GenerationExecuteSimpleDto.properties.errorTrace") { doesNotExist() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.domainId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.domain") { doesNotExist() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.lieId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.name") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.woundTimingId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.woundPlaceId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.woundAgentId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.woundFormId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.woundDurationId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.opennessStageId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.speechRegisterId") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.speechRegister") {
                doesNotExist()
            }
            // refId까지 선택 항목이 되어 필수 필드가 하나도 없다.
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.required") { doesNotExist() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.refId") { exists() }
            jsonPath("$.components.schemas.PromptTemplateDto.properties.type") { exists() }
            jsonPath("$.components.schemas.DefaultPromptTemplateDto.properties.promptTemplateId") { exists() }
            jsonPath("$.paths['/api/v1/default-prompt-templates'].get") { exists() }
            jsonPath("$.paths['/api/v1/default-prompt-templates/{type}'].get") { exists() }
            jsonPath("$.paths['/api/v1/default-prompt-templates/{type}'].put") { exists() }
            jsonPath("$.paths['/api/v1/prompt-templates'].get.parameters[?(@.name == 'type')]") { exists() }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.gender.enum[0]") {
                value("MALE")
            }
            jsonPath("$.components.schemas.L0CharacterGenerationRequest.properties.gender.enum[1]") {
                value("FEMALE")
            }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].post") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].post.responses['429']") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations'].get.parameters[?(@.name == 'sort')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/generations/{generationId}'].get") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters'].get") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters'].get.parameters[?(@.name == 'page')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters'].get.parameters[?(@.name == 'size')]") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/{characterId}'].get") { exists() }
            jsonPath("$.paths['/api/v1/l0-characters/{characterId}'].delete") { exists() }
            jsonPath("$.components.schemas.CharacterSimpleDto") { exists() }
            jsonPath("$.components.schemas.CharacterSimpleDto.properties.identity") { doesNotExist() }
            jsonPath("$.components.schemas.CharacterDto") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.name") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.lieType") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.speechArchetypeNote") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.concreteDetails") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.disclosureItems") { exists() }
            jsonPath("$.components.schemas.CharacterDto.properties.identity") { doesNotExist() }
            jsonPath("$.components.schemas.CharacterDto.properties.core") { doesNotExist() }
            jsonPath("$.components.schemas.CharacterDto.properties.voice") { doesNotExist() }
            jsonPath("$.components.schemas.CharacterConcreteDetailDto") { exists() }
            jsonPath("$.components.schemas.CharacterDisclosureItemDto") { exists() }
        }
    }

    @Test
    fun `BRANCH를 편집하고 VERSION을 발행하며 참조 무결성을 지킨다`() {
        val templateId = createTemplate("L0 Character", PromptTemplateType.CHARACTER_GENERATION)
        val otherTemplateId = createTemplate("Other Character", PromptTemplateType.CHARACTER_GENERATION)

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
            jsonPath("$.content[0].instructions") { doesNotExist() }
            jsonPath("$.content[0].inputs") { doesNotExist() }
        }

        mockMvc.get("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "experiment").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(copiedBranchId) }
            jsonPath("$.name") { value("experiment") }
            jsonPath("$.instructions") { value("Generate character") }
            jsonPath("$.inputs.length()") { value(2) }
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
        val templateId = createTemplate("Generation Template", PromptTemplateType.CHARACTER_GENERATION)
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
    fun `생성 요청은 refId 외의 입력을 생략할 수 있다`() {
        val templateId = createTemplate("Nullable Generation Input Template", PromptTemplateType.CHARACTER_GENERATION)
        val branchId = createBranch(templateId, "main")
        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"main","instructions":"System","inputs":[{"role":"user","inputText":"{{DOMAIN}}"}],"revision":0}"""
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refId":$branchId}"""
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.refId") { value(branchId) }
            jsonPath("$.status") { value("PREPARING") }
            jsonPath("$.domainId") { doesNotExist() }
            jsonPath("$.age") { doesNotExist() }
            jsonPath("$.gender") { doesNotExist() }
            jsonPath("$.speechRegisterId") { doesNotExist() }
        }
    }

    @Test
    fun `생성 실행 이력을 페이징 검색하고 단일 상세 조회한다`() {
        val templateId = createTemplate("Generation History Template", PromptTemplateType.CHARACTER_GENERATION)
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
            jsonPath("$.content[0].promptRefSnapshotJson") { doesNotExist() }
            jsonPath("$.content[0].userInputJson") { doesNotExist() }
            jsonPath("$.content[0].llmOutput") { doesNotExist() }
            jsonPath("$.content[0].errorTrace") { doesNotExist() }
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
        val templateId = createTemplate("Version Sequence Template", PromptTemplateType.CHARACTER_GENERATION)
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
    fun `첫 BRANCH를 기본값으로 지정하고 템플릿에서 기본 BRANCH를 변경한다`() {
        val templateId = createTemplate("Default Branch Template", PromptTemplateType.CHARACTER_GENERATION)

        mockMvc.get("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { doesNotExist() }
        }
        mockMvc.post("/api/v1/prompt-templates/{promptTemplateId}/refs/branches", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"feature/new"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }

        createBranch(templateId, "main")
        createBranch(templateId, "candidate_2")
        mockMvc.get("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { value("main") }
        }

        mockMvc.put("/api/v1/prompt-templates/{id}", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Default Branch Template","defaultBranch":"candidate_2"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { value("candidate_2") }
        }
        mockMvc.put("/api/v1/prompt-templates/{id}", templateId) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Default Branch Template","defaultBranch":"missing"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }

        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "candidate_2") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"release-2","instructions":"","inputs":[],"revision":0}"""
        }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { value("release-2") }
        }

        mockMvc.delete("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", templateId, "release-2").andExpect {
            status { isNoContent() }
        }
        mockMvc.get("/api/v1/prompt-templates/{id}", templateId).andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { doesNotExist() }
        }
    }

    @Test
    fun `캐릭터 생성에 필요한 모든 축 정보를 한 번에 조회한다`() {
        referenceDataInitializer.run(DefaultApplicationArguments())
        assertEquals(8, domainRepository.count())
        assertEquals(12, lieRepository.count())
        assertEquals(4, woundTimingRepository.count())
        assertEquals(5, woundPlaceRepository.count())
        assertEquals(7, woundAgentRepository.count())
        assertEquals(6, woundFormRepository.count())
        assertEquals(4, woundDurationRepository.count())
        assertEquals(25, personalityTraitStageRepository.count())
        assertEquals(6, attachmentStyleRepository.count())
        assertEquals(6, emotionalExpressionRepository.count())
        assertEquals(6, avoidanceStyleRepository.count())
        assertEquals(8, speechArchetypeRepository.count())
        assertEquals(3, speechRegisterRepository.count())
        val opennessStageThree = personalityTraitStageRepository.findAll().single {
            it.trait == BigFiveTrait.OPENNESS && it.stage == 3
        }
        assertEquals(
            "이 축은 이 인물을 설명하는 특징이 아니다. 여기서 파생되는 행동을 만들지 마라.",
            opennessStageThree.description,
        )
        mockMvc.get("/api/v1/l0-characters/generation-options").andExpect {
            status { isOk() }
            jsonPath("$.domains.length()") { value(8) }
            jsonPath("$.lies.length()") { value(12) }
            jsonPath("$.woundTimings.length()") { value(4) }
            jsonPath("$.woundPlaces.length()") { value(5) }
            jsonPath("$.woundAgents.length()") { value(7) }
            jsonPath("$.woundForms.length()") { value(6) }
            jsonPath("$.woundDurations.length()") { value(4) }
            jsonPath("$.woundTimings[0].value") { value("유년") }
            jsonPath("$.woundAgents[6].value") { value("특정인 없음") }
            jsonPath("$.woundDurations[3].value") { value("수년간 서서히") }
            jsonPath("$.personalityTraitStages.length()") { value(25) }
            jsonPath("$.attachments.length()") { value(6) }
            jsonPath("$.emotionalExpressions.length()") { value(6) }
            jsonPath("$.avoidanceStyles.length()") { value(6) }
            jsonPath("$.speechArchetypes.length()") { value(8) }
            jsonPath("$.speechRegisters.length()") { value(3) }
            jsonPath("$.speechRegisters[0].name") { value("반말 기본") }
            jsonPath("$.speechRegisters[1].name") { value("존댓말 기본") }
            jsonPath("$.speechRegisters[2].name") { value("처음엔 존댓말 후 반말") }
        }
    }

    @Test
    fun `L0 캐릭터 프롬프트 BRANCH와 복사된 VERSION을 초기화한다`() {
        referenceDataInitializer.run(DefaultApplicationArguments())

        val template = assertNotNull(promptTemplateRepository.findByName(L0_CHARACTER_PROMPT_TEMPLATE_NAME))
        assertEquals(4, template.lastVersion)
        assertEquals(L0_CHARACTER_PROMPT_BRANCH_NAME, template.defaultBranch)
        assertEquals(PromptTemplateType.CHARACTER_GENERATION, template.type)

        val refs = promptTemplateRefRepository.findAll().filter { it.promptTemplate.id == template.id }
        assertEquals(5, refs.size)
        val branch = refs.single { it.type == PromptTemplateRefType.BRANCH }
        val versionOne = refs.single { it.version == 1L }
        val versionTwo = refs.single { it.version == 2L }
        val versionThree = refs.single { it.version == 3L }
        val versionFour = refs.single { it.version == 4L }

        assertEquals(L0_CHARACTER_PROMPT_BRANCH_NAME, branch.name)
        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS_V4, branch.instructions)
        assertEquals(1, branch.inputs.size)
        assertEquals("user", branch.inputs.single().role)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT_V4, branch.inputs.single().inputText)

        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS, versionOne.instructions)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT, versionOne.inputs.single().inputText)
        assertEquals(branch.id, versionOne.fromRef?.id)
        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_1, versionTwo.instructions)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT_V3_1, versionTwo.inputs.single().inputText)
        assertEquals(branch.id, versionTwo.fromRef?.id)
        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS_V3_2, versionThree.instructions)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT_V3_2, versionThree.inputs.single().inputText)
        assertEquals(branch.id, versionThree.fromRef?.id)
        assertEquals(L0_CHARACTER_PROMPT_INSTRUCTIONS_V4, versionFour.instructions)
        assertEquals(L0_CHARACTER_PROMPT_INPUT_TEXT_V4, versionFour.inputs.single().inputText)
        assertEquals(branch.id, versionFour.fromRef?.id)
        assertEquals(branch.revision, versionFour.fromRefRevision)

        mockMvc.get("/api/v1/prompt-templates/{id}", template.id).andExpect {
            status { isOk() }
            jsonPath("$.defaultBranch") { value(L0_CHARACTER_PROMPT_BRANCH_NAME) }
        }
        mockMvc.get("/api/v1/prompt-templates") {
            param("name", L0_CHARACTER_PROMPT_TEMPLATE_NAME)
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].defaultBranch") { value(L0_CHARACTER_PROMPT_BRANCH_NAME) }
        }
    }

    @Test
    fun `refId를 생략하면 기본 캐릭터 생성 프롬프트로 실행한다`() {
        val defaultTemplate = assertNotNull(promptTemplateRepository.findByName(L0_CHARACTER_PROMPT_TEMPLATE_NAME))
        val defaultBranch = assertNotNull(
            promptTemplateRefRepository.findOneWithDetailsByPromptTemplateIdAndName(
                defaultTemplate.id,
                L0_CHARACTER_PROMPT_BRANCH_NAME,
            ),
        )

        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = generationRequest(null)
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.refId") { value(defaultBranch.id) }
            jsonPath("$.status") { value("PREPARING") }
        }
    }

    @Test
    fun `용도가 다른 ref로 생성을 요청하면 400을 반환한다`() {
        val runtimeTemplateId = createTemplate("Runtime Only Template", PromptTemplateType.CHARACTER_RUNTIME)
        val branchId = createBranch(runtimeTemplateId, "main")
        mockMvc.put("/api/v1/prompt-templates/{promptTemplateId}/refs/{refName}", runtimeTemplateId, "main") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"main","instructions":"런타임 지시","inputs":[{"role":"user","inputText":"입력"}],"revision":0}"""
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/l0-characters/generations") {
            contentType = MediaType.APPLICATION_JSON
            content = generationRequest(branchId)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    private fun createTemplate(name: String, type: PromptTemplateType): Long = locationId(
        mockMvc.post("/api/v1/prompt-templates") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name","type":"$type"}"""
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

    private fun generationRequest(refId: Long?): String {
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
        // refId가 null이면 필드 자체를 빼서 기본 프롬프트로 실행되는 경로를 검증한다.
        val refIdField = refId?.let { """"refId":$it,""" } ?: ""
        return """
        {
          $refIdField
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
}
