package ltmp.charactergen.api.prompt

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.PromptTemplate
import ltmp.charactergen.data.entity.PromptTemplateRef
import java.time.OffsetDateTime

@Schema(description = "프롬프트 템플릿 생성 요청")
data class CreatePromptTemplateRequest(
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "템플릿 이름", example = "L0 캐릭터 생성", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)

@Schema(description = "프롬프트 템플릿 수정 요청")
data class UpdatePromptTemplateRequest(
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "변경할 템플릿 이름", example = "L0 캐릭터 생성", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)

@Schema(description = "프롬프트 템플릿 정보")
data class PromptTemplateDto(
    @field:Schema(description = "템플릿 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "템플릿 이름", example = "L0 캐릭터 생성", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "마지막으로 발급한 VERSION 번호", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    val lastVersion: Long,
    @field:Schema(description = "생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "BRANCH ref 생성 요청")
data class CreatePromptTemplateBranchRequest(
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "BRANCH 이름", example = "main", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "내용을 복사할 동일 템플릿의 ref 식별자. null이면 빈 BRANCH를 만든다.", nullable = true)
    val fromRefId: Long? = null,
)

@Schema(description = "VERSION ref 생성 요청")
data class CreatePromptTemplateVersionRefRequest(
    @field:Schema(description = "발행할 동일 템플릿의 BRANCH ref 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val fromRefId: Long,
)

@Schema(description = "BRANCH ref 수정 요청")
data class UpdatePromptTemplateBranchRequest(
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "BRANCH 이름", example = "main", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Schema(description = "LLM 지시사항. 중간 저장에서는 빈 문자열을 허용한다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val instructions: String,
    @field:Valid
    @field:Schema(description = "순서대로 저장할 LLM 입력 목록. 중간 저장에서는 빈 목록을 허용한다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val inputs: List<PromptTemplateInputRequest>,
    @field:PositiveOrZero
    @field:Schema(description = "조회 시 받은 낙관적 잠금 revision", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    val revision: Long,
)

@Schema(description = "프롬프트 템플릿 입력")
data class PromptTemplateInputRequest(
    @field:NotBlank
    @field:Size(max = 50)
    @field:Schema(description = "LLM 입력 역할", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
    val role: String,
    @field:Schema(description = "입력 텍스트. BRANCH 중간 저장에서는 빈 문자열을 허용한다.", requiredMode = Schema.RequiredMode.REQUIRED)
    val inputText: String,
)

@Schema(description = "프롬프트 템플릿 ref 정보")
data class PromptTemplateRefDto(
    @field:Schema(description = "ref 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "소속 템플릿 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val promptTemplateId: Long,
    @field:Schema(description = "소속 템플릿 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val promptTemplateName: String,
    @field:Schema(description = "ref 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: PromptTemplateRefType,
    @field:Schema(description = "BRANCH 이름. VERSION이면 null", nullable = true)
    val name: String? = null,
    @field:Schema(description = "템플릿별 VERSION 번호. BRANCH이면 null", nullable = true)
    val version: Long? = null,
    @field:Schema(description = "복제 원본 ref 식별자", nullable = true)
    val fromRefId: Long? = null,
    @field:Schema(description = "복제 당시 원본 ref revision", nullable = true)
    val fromRefRevision: Long? = null,
    @field:Schema(description = "LLM 지시사항", requiredMode = Schema.RequiredMode.REQUIRED)
    val instructions: String,
    @field:Schema(description = "순서가 보존된 LLM 입력", requiredMode = Schema.RequiredMode.REQUIRED)
    val inputs: List<PromptTemplateInputDto>,
    @field:Schema(description = "낙관적 잠금 revision", requiredMode = Schema.RequiredMode.REQUIRED)
    val revision: Long,
    @field:Schema(description = "생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,
    @field:Schema(description = "최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "저장된 프롬프트 입력")
data class PromptTemplateInputDto(
    @field:Schema(description = "입력 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @field:Schema(description = "LLM 입력 역할", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
    val role: String,
    @field:Schema(description = "입력 텍스트", requiredMode = Schema.RequiredMode.REQUIRED)
    val inputText: String,
)

fun PromptTemplate.toDto() = PromptTemplateDto(
    id = id,
    name = name,
    lastVersion = lastVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PromptTemplateRef.toDto() = PromptTemplateRefDto(
    id = id,
    promptTemplateId = promptTemplate.id,
    promptTemplateName = promptTemplate.name,
    type = type,
    name = name,
    version = version,
    fromRefId = fromRef?.id,
    fromRefRevision = fromRefRevision,
    instructions = instructions,
    inputs = inputs.map { PromptTemplateInputDto(id = it.id, role = it.role, inputText = it.inputText) },
    revision = revision,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
