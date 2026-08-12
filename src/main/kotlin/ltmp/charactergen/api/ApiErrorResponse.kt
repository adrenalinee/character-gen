package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "API 오류 응답")
data class ApiErrorResponse(
    @field:Schema(description = "HTTP 상태 코드", example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: Int,
    @field:Schema(description = "오류 코드", example = "RESOURCE_NOT_FOUND", requiredMode = Schema.RequiredMode.REQUIRED)
    val code: String,
    @field:Schema(description = "오류 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    val message: String,
    @field:Schema(description = "오류 발생 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val timestamp: OffsetDateTime,
)
