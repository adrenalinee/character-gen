package ltmp.charactergen.api.prompt

import ltmp.charactergen.config.L0_CHARACTER_PROMPT_BRANCH_NAME
import ltmp.charactergen.config.L0_CHARACTER_PROMPT_TEMPLATE_NAME
import ltmp.charactergen.config.L0_RUNTIME_INSTRUCTION_BRANCH_NAME
import ltmp.charactergen.config.L0_RUNTIME_INSTRUCTION_TEMPLATE_NAME
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.repo.DefaultPromptTemplateRepository
import ltmp.charactergen.data.repo.PromptTemplateRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
class DefaultPromptTemplateIntegrationTests(
    private val mockMvc: MockMvc,
    private val promptTemplateRepository: PromptTemplateRepository,
    private val defaultPromptTemplateRepository: DefaultPromptTemplateRepository,
) {
    @Test
    fun `초기화 데이터가 두 용도의 기본 프롬프트를 지정한다`() {
        val generationTemplate = assertNotNull(
            promptTemplateRepository.findByName(L0_CHARACTER_PROMPT_TEMPLATE_NAME),
        )
        val runtimeTemplate = assertNotNull(
            promptTemplateRepository.findByName(L0_RUNTIME_INSTRUCTION_TEMPLATE_NAME),
        )
        assertEquals(PromptTemplateType.CHARACTER_GENERATION, generationTemplate.type)
        assertEquals(PromptTemplateType.CHARACTER_RUNTIME, runtimeTemplate.type)

        mockMvc.get("/api/v1/default-prompt-templates").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(PromptTemplateType.entries.size) }
        }
        mockMvc.get("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_GENERATION).andExpect {
            status { isOk() }
            jsonPath("$.promptTemplateId") { value(generationTemplate.id) }
            jsonPath("$.promptTemplateName") { value(L0_CHARACTER_PROMPT_TEMPLATE_NAME) }
            jsonPath("$.defaultBranch") { value(L0_CHARACTER_PROMPT_BRANCH_NAME) }
        }
        mockMvc.get("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_RUNTIME).andExpect {
            status { isOk() }
            jsonPath("$.promptTemplateId") { value(runtimeTemplate.id) }
            jsonPath("$.promptTemplateName") { value(L0_RUNTIME_INSTRUCTION_TEMPLATE_NAME) }
            jsonPath("$.defaultBranch") { value(L0_RUNTIME_INSTRUCTION_BRANCH_NAME) }
        }
    }

    @Test
    fun `같은 용도의 템플릿으로만 기본 프롬프트를 바꿀 수 있다`() {
        val replacement = promptTemplateRepository.saveAndFlush(
            PromptTemplate(
                name = "대체 런타임 템플릿",
                type = PromptTemplateType.CHARACTER_RUNTIME,
                defaultBranch = "main",
            ),
        )

        mockMvc.put("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_RUNTIME) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"promptTemplateId":${replacement.id}}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.promptTemplateId") { value(replacement.id) }
            jsonPath("$.promptTemplateName") { value("대체 런타임 템플릿") }
        }
        assertEquals(
            replacement.id,
            assertNotNull(
                defaultPromptTemplateRepository.findOneWithTemplateByType(PromptTemplateType.CHARACTER_RUNTIME),
            ).promptTemplate.id,
        )

        // 용도가 다른 템플릿은 기본값이 될 수 없다.
        mockMvc.put("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_GENERATION) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"promptTemplateId":${replacement.id}}"""
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.put("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_RUNTIME) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"promptTemplateId":999999}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `템플릿 검색을 용도로 거를 수 있다`() {
        promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "런타임 검색 대상", type = PromptTemplateType.CHARACTER_RUNTIME),
        )

        mockMvc.get("/api/v1/prompt-templates") {
            param("type", PromptTemplateType.CHARACTER_RUNTIME.name)
            param("size", "100")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[?(@.type != 'CHARACTER_RUNTIME')]") { isEmpty() }
            jsonPath("$.content[?(@.name == '런타임 검색 대상')]") { isNotEmpty() }
            jsonPath("$.content[?(@.name == '$L0_CHARACTER_PROMPT_TEMPLATE_NAME')]") { isEmpty() }
        }
    }

    @Test
    fun `기본 프롬프트로 지정된 템플릿은 삭제할 수 없다`() {
        val template = promptTemplateRepository.saveAndFlush(
            PromptTemplate(name = "삭제 대상 런타임 템플릿", type = PromptTemplateType.CHARACTER_RUNTIME),
        )
        mockMvc.put("/api/v1/default-prompt-templates/{type}", PromptTemplateType.CHARACTER_RUNTIME) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"promptTemplateId":${template.id}}"""
        }.andExpect { status { isOk() } }

        mockMvc.delete("/api/v1/prompt-templates/{id}", template.id).andExpect {
            status { isConflict() }
        }
    }
}
