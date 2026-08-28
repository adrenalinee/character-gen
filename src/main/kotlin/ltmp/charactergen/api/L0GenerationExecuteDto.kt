package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.media.Schema
import ltmp.charactergen.data.Gender
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

    @field:Schema(description = "실행에 사용한 활동 분야 옵션 식별자", example = "1", nullable = true)
    val domainId: Long? = null,

    @field:Schema(description = "실행에 사용한 Lie 옵션 식별자", example = "1", nullable = true)
    val lieId: Long? = null,

    @field:Schema(description = "실행에 사용한 상처 시기 옵션 식별자", example = "1", nullable = true)
    val woundTimingId: Long? = null,

    @field:Schema(description = "실행에 사용한 상처 장소 옵션 식별자", example = "1", nullable = true)
    val woundPlaceId: Long? = null,

    @field:Schema(description = "실행에 사용한 상처를 준 사람 옵션 식별자", example = "1", nullable = true)
    val woundAgentId: Long? = null,

    @field:Schema(description = "실행에 사용한 상처 형태 옵션 식별자", example = "1", nullable = true)
    val woundFormId: Long? = null,

    @field:Schema(description = "실행에 사용한 상처 지속 시간 옵션 식별자", example = "1", nullable = true)
    val woundDurationId: Long? = null,

    @field:Schema(description = "실행에 사용한 개방성 단계 옵션 식별자", example = "3", nullable = true)
    val opennessStageId: Long? = null,

    @field:Schema(description = "실행에 사용한 성실성 단계 옵션 식별자", example = "8", nullable = true)
    val conscientiousnessStageId: Long? = null,

    @field:Schema(description = "실행에 사용한 외향성 단계 옵션 식별자", example = "13", nullable = true)
    val extraversionStageId: Long? = null,

    @field:Schema(description = "실행에 사용한 우호성 단계 옵션 식별자", example = "18", nullable = true)
    val agreeablenessStageId: Long? = null,

    @field:Schema(description = "실행에 사용한 신경성 단계 옵션 식별자", example = "23", nullable = true)
    val neuroticismStageId: Long? = null,

    @field:Schema(description = "실행에 사용한 관계 태도 옵션 식별자", example = "1", nullable = true)
    val attachmentStyleId: Long? = null,

    @field:Schema(description = "실행에 사용한 정서 표출 옵션 식별자", example = "1", nullable = true)
    val emotionalExpressionId: Long? = null,

    @field:Schema(description = "실행에 사용한 회피 방식 옵션 식별자", example = "1", nullable = true)
    val avoidanceStyleId: Long? = null,

    @field:Schema(description = "실행에 사용한 말투 원형 옵션 식별자", example = "1", nullable = true)
    val speechArchetypeId: Long? = null,

    @field:Schema(description = "캐릭터에 관한 운영자 설명", nullable = true)
    val description: String? = null,

    @field:Schema(description = "프롬프트에 주입한 캐릭터 이름", example = "서윤", nullable = true)
    val name: String? = null,

    @field:Schema(description = "캐릭터의 연령", example = "29", nullable = true)
    val age: Int? = null,

    @field:Schema(description = "캐릭터의 성별", example = "FEMALE", nullable = true)
    val gender: Gender? = null,

    @field:Schema(description = "존댓말/반말 기본 정책 옵션 식별자", example = "1", nullable = true)
    val speechRegisterId: Long? = null,

    @field:Schema(description = "서사 스케일", minimum = "1", maximum = "5", nullable = true)
    val scale: Int? = null,

    @field:Schema(description = "감정 강도", minimum = "1", maximum = "5", nullable = true)
    val emotion: Int? = null,

    @field:Schema(description = "추가 생성 제약", nullable = true)
    val extraConstraints: String? = null,

    @field:Schema(
        description = "이 실행에서 프롬프트에 알려준 기존 캐릭터 목록. 한 줄에 한 명이며, 알려준 캐릭터가 없으면 null이다.",
        nullable = true,
    )
    val existingCharactersNote: String? = null,

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

    @field:Schema(
        description = "저장 시점에 발견한 캐릭터 데이터 경고. 한 줄에 하나이며, 경고가 없으면 null이다. " +
            "개수 규정 미달과, 다른 필드와 겹치는데도 최소 개수 때문에 런타임에서 빼지 못한 항목이 여기 담긴다. " +
            "경고가 있어도 캐릭터는 그대로 저장되며 대화도 가능하다.",
        nullable = true,
        example = "절대 하지 않는 것(forbidden_behaviors) 개수가 규정과 다릅니다: 규정 3~5개, 실제 2개",
    )
    val runtimeWarnings: String? = null,

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
    domainId = domain?.id,
    lieId = lie?.id,
    woundTimingId = woundTiming?.id,
    woundPlaceId = woundPlace?.id,
    woundAgentId = woundAgent?.id,
    woundFormId = woundForm?.id,
    woundDurationId = woundDuration?.id,
    opennessStageId = opennessStage?.id,
    conscientiousnessStageId = conscientiousnessStage?.id,
    extraversionStageId = extraversionStage?.id,
    agreeablenessStageId = agreeablenessStage?.id,
    neuroticismStageId = neuroticismStage?.id,
    attachmentStyleId = attachmentStyle?.id,
    emotionalExpressionId = emotionalExpression?.id,
    avoidanceStyleId = avoidanceStyle?.id,
    speechArchetypeId = speechArchetype?.id,
    description = description,
    name = name,
    age = age,
    gender = gender,
    speechRegisterId = speechRegister?.id,
    scale = scale,
    emotion = emotion,
    extraConstraints = extraConstraints,
    existingCharactersNote = existingCharactersNote,
    promptRefSnapshotJson = promptRefSnapshotJson,
    userInputJson = userInputJson,
    llmOutput = llmOutput,
    characterId = character?.id,
    status = status,
    errorTrace = errorTrace,
    runtimeWarnings = runtimeWarnings,
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
    characterId = character?.id,
    status = status,
    message = message,
    elapsed = elapsed,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
