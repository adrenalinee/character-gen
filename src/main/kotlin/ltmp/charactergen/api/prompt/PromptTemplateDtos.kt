package ltmp.charactergen.api.prompt

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ltmp.charactergen.data.prompt.PromptTemplate
import ltmp.charactergen.data.prompt.PromptTemplateVersion
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

    @field:Schema(description = "생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(description = "프롬프트 템플릿 버전 생성 요청")
data class CreatePromptTemplateVersionRequest(
    @field:Schema(description = "소속 템플릿 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val promptTemplateId: Long,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "버전 이름", example = "v3", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:NotBlank
    @field:Schema(description = "해당 버전에서 실행할 실제 프롬프트", requiredMode = Schema.RequiredMode.REQUIRED)
    val prompt: String,
)

@Schema(description = "프롬프트 템플릿 버전 정보")
data class PromptTemplateVersionDto(
    @field:Schema(description = "버전 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,

    @field:Schema(description = "소속 템플릿 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val promptTemplateId: Long,

    @field:Schema(description = "소속 템플릿 이름", example = "L0 캐릭터 생성", requiredMode = Schema.RequiredMode.REQUIRED)
    val promptTemplateName: String,

    @field:Schema(description = "버전 이름", example = "v3", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,

    @field:Schema(description = "해당 버전에서 실행할 실제 프롬프트", requiredMode = Schema.RequiredMode.REQUIRED)
    val prompt: String,

    @field:Schema(description = "생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

fun PromptTemplate.toDto() = PromptTemplateDto(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PromptTemplateVersion.toDto() = PromptTemplateVersionDto(
    id = id,
    promptTemplateId = promptTemplate.id,
    promptTemplateName = promptTemplate.name,
    name = name,
    prompt = prompt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
