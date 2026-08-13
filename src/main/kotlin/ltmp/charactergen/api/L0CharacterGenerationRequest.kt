package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import ltmp.charactergen.data.Gender

@Schema(description = "L0 캐릭터 생성 프롬프트에 사용할 사용자 입력. refId 외의 필드는 모두 선택 사항이다.")
data class L0CharacterGenerationRequest(
    @field:Positive
    @field:Schema(
        description = "실행할 BRANCH 또는 VERSION 프롬프트 ref 식별자",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val refId: Long,

    @field:Schema(
        description = "캐릭터에 관한 운영자 설명. 비어 있으면 프롬프트에 빈 문자열을 주입한다.",
        nullable = true,
    )
    val description: String? = null,

    @field:Positive
    @field:Schema(description = "활동 분야 옵션 식별자", example = "1", nullable = true)
    val domainId: Long? = null,

    @field:Positive
    @field:Schema(
        description = "Lie 옵션 식별자. 유형과 핵심 명제는 서버가 조회해 주입한다.",
        example = "1",
        nullable = true,
    )
    val lieId: Long? = null,

    @field:Positive
    @field:Schema(description = "개방성 단계 옵션 식별자", example = "3", nullable = true)
    val opennessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "성실성 단계 옵션 식별자", example = "8", nullable = true)
    val conscientiousnessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "외향성 단계 옵션 식별자", example = "13", nullable = true)
    val extraversionStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "우호성 단계 옵션 식별자", example = "18", nullable = true)
    val agreeablenessStageId: Long? = null,

    @field:Positive
    @field:Schema(description = "신경성 단계 옵션 식별자", example = "23", nullable = true)
    val neuroticismStageId: Long? = null,

    @field:Positive
    @field:Schema(
        description = "관계 태도 옵션 식별자. 유형과 설명은 서버가 조회해 주입한다.",
        example = "1",
        nullable = true,
    )
    val attachmentStyleId: Long? = null,

    @field:Positive
    @field:Schema(
        description = "정서 표출 옵션 식별자. 유형과 설명은 서버가 조회해 주입한다.",
        example = "1",
        nullable = true,
    )
    val emotionalExpressionId: Long? = null,

    @field:Positive
    @field:Schema(
        description = "회피 방식 옵션 식별자. 유형과 설명은 서버가 조회해 주입한다.",
        example = "1",
        nullable = true,
    )
    val avoidanceStyleId: Long? = null,

    @field:Positive
    @field:Schema(
        description = "말투 원형 옵션 식별자. 이름과 설명은 서버가 조회해 주입한다.",
        example = "1",
        nullable = true,
    )
    val speechArchetypeId: Long? = null,

    @field:Positive
    @field:Schema(description = "캐릭터의 연령", example = "29", nullable = true)
    val age: Int? = null,

    @field:Schema(description = "캐릭터의 성별", example = "FEMALE", nullable = true)
    val gender: Gender? = null,

    @field:Positive
    @field:Schema(description = "존댓말/반말 기본 정책 옵션 식별자", example = "1", nullable = true)
    val speechRegisterId: Long? = null,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(
        description = "서사 스케일. 1은 일상적 사건, 5는 인생을 바꾸는 사건이다.",
        example = "2",
        nullable = true,
    )
    val scale: Int? = null,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(
        description = "감정 강도. 1은 담담한 반응, 5는 격한 반응이다.",
        example = "4",
        nullable = true,
    )
    val emotion: Int? = null,

    @field:Schema(
        description = "추가 생성 제약. 비어 있으면 프롬프트에 빈 문자열을 주입한다.",
        nullable = true,
    )
    val extraConstraints: String? = null,
)
