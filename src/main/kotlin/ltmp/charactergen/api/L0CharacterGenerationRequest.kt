package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

@Schema(description = "L0 캐릭터 생성 프롬프트에 사용할 사용자 입력")
data class L0CharacterGenerationRequest(
    @field:Schema(
        description = "캐릭터에 관한 운영자 설명. 비어 있으면 서버가 '없음'을 주입한다.",
        nullable = true,
    )
    val description: String? = null,

    @field:NotBlank
    @field:Schema(description = "활동 분야 카테고리", example = "웹툰")
    val domain: String,

    @field:NotBlank
    @field:Schema(description = "Lie 카탈로그의 유형. 핵심 명제는 서버가 주입한다.", example = "대체 가능성 공포형")
    val lieType: String,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "개방성 단계. 설명은 서버가 주입한다.", defaultValue = "3")
    val opennessStage: Int = 3,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "성실성 단계. 설명은 서버가 주입한다.", defaultValue = "3")
    val conscientiousnessStage: Int = 3,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "외향성 단계. 설명은 서버가 주입한다.", defaultValue = "3")
    val extraversionStage: Int = 3,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "우호성 단계. 설명은 서버가 주입한다.", defaultValue = "3")
    val agreeablenessStage: Int = 3,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "신경성 단계. 설명은 서버가 주입한다.", defaultValue = "3")
    val neuroticismStage: Int = 3,

    @field:NotBlank
    @field:Schema(description = "관계 태도 카탈로그의 유형. 설명은 서버가 주입한다.", example = "일정 거리 유지")
    val attachment: String,

    @field:NotBlank
    @field:Schema(description = "정서 표출 카탈로그의 유형. 설명은 서버가 주입한다.", example = "사실 치환")
    val emotionalExpression: String,

    @field:NotBlank
    @field:Schema(description = "회피 방식 카탈로그의 유형. 설명은 서버가 주입한다.", example = "짧게 답하고 침묵")
    val avoidanceStyle: String,

    @field:NotBlank
    @field:Schema(description = "말투 원형 카탈로그의 이름. 설명은 서버가 주입한다.", example = "비유형")
    val speechArchetype: String,

    @field:Positive
    @field:Schema(description = "캐릭터의 연령", example = "29")
    val age: Int,

    @field:NotBlank
    @field:Schema(description = "캐릭터의 성별", example = "여성")
    val gender: String,

    @field:NotBlank
    @field:Schema(description = "존댓말/반말 기본 정책", example = "기본적으로 존댓말")
    val speechRegister: String,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "서사 스케일. 1은 일상적 사건, 5는 인생을 바꾸는 사건이다.", example = "2")
    val scale: Int,

    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "감정 강도. 1은 담담한 반응, 5는 격한 반응이다.", example = "4")
    val emotion: Int,

    @field:Schema(
        description = "추가 생성 제약. 비어 있으면 서버가 '없음'을 주입한다.",
        nullable = true,
    )
    val extraConstraints: String? = null,
)
