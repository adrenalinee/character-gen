package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.L0GenerationExecuteStatus
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.data.entity.L0GenerationExecute
import java.time.OffsetDateTime

@Schema(description = "L0 캐릭터 생성 실행 정보")
data class L0GenerationExecuteDto(
    @field:Schema(description = "실행 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,

    @field:Schema(description = "실행에 사용한 프롬프트 ref 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val refId: Long,

    @field:Schema(description = "실행에 사용한 ref 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    val refType: PromptTemplateRefType,

    @field:Schema(description = "BRANCH 실행 시점의 ref 원본 JSON. VERSION 실행이면 null", nullable = true)
    val promptRefSnapshotJson: String? = null,

    @field:Schema(description = "API로 전달되어 저장된 사용자 입력 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    val userInputJson: String,

    @field:Schema(description = "LLM 원본 출력. 실행 완료 전에는 null이다.", nullable = true)
    val llmOutput: String? = null,

    @field:Schema(description = "이 실행으로 생성된 캐릭터 식별자. 생성 완료 전에는 null이다.", example = "100", nullable = true)
    val characterId: Long? = null,

    @field:Schema(description = "현재 실행 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: L0GenerationExecuteStatus,

    @field:Schema(description = "실패 시 오류 추적 정보", nullable = true)
    val errorTrace: String? = null,

    @field:Schema(description = "실행 상태에 관한 부가 메시지", nullable = true)
    val message: String? = null,

    @field:Schema(description = "LLM 호출에 걸린 시간(밀리초)", example = "1523", nullable = true)
    val elapsed: Long? = null,

    @field:Schema(description = "COMPLETED 상태가 된 시각", nullable = true)
    val completedAt: OffsetDateTime? = null,

    @field:Schema(description = "실행 레코드 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "실행 레코드 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

@Schema(
    description = "L0 캐릭터 생성 실행 검색 목록 정보. 프롬프트 스냅샷, 사용자 입력, LLM 출력, 오류 추적 원문은 상세 조회에서만 제공한다.",
)
data class L0GenerationExecuteSimpleDto(
    @field:Schema(description = "실행 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,

    @field:Schema(description = "실행에 사용한 프롬프트 ref 식별자", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    val refId: Long,

    @field:Schema(description = "실행에 사용한 ref 타입", requiredMode = Schema.RequiredMode.REQUIRED)
    val refType: PromptTemplateRefType,

    @field:Schema(description = "이 실행으로 생성된 캐릭터 식별자. 생성 완료 전에는 null이다.", example = "100", nullable = true)
    val characterId: Long? = null,

    @field:Schema(description = "현재 실행 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    val status: L0GenerationExecuteStatus,

    @field:Schema(description = "실행 상태에 관한 부가 메시지", nullable = true)
    val message: String? = null,

    @field:Schema(description = "LLM 호출에 걸린 시간(밀리초)", example = "1523", nullable = true)
    val elapsed: Long? = null,

    @field:Schema(description = "COMPLETED 상태가 된 시각", nullable = true)
    val completedAt: OffsetDateTime? = null,

    @field:Schema(description = "실행 레코드 생성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val createdAt: OffsetDateTime,

    @field:Schema(description = "실행 레코드 최종 수정 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    val updatedAt: OffsetDateTime,
)

fun L0GenerationExecute.toDto() = L0GenerationExecuteDto(
    id = id,
    refId = promptTemplateRef.id,
    refType = promptTemplateRef.type,
    promptRefSnapshotJson = promptRefSnapshotJson,
    userInputJson = userInputJson,
    llmOutput = llmOutput,
    characterId = characterId,
    status = status,
    errorTrace = errorTrace,
    message = message,
    elapsed = elapsed,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun L0GenerationExecute.toSimpleDto() = L0GenerationExecuteSimpleDto(
    id = id,
    refId = promptTemplateRef.id,
    refType = promptTemplateRef.type,
    characterId = characterId,
    status = status,
    message = message,
    elapsed = elapsed,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
