package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.axis.L0CharacterGenerationOptionsDto
import ltmp.charactergen.service.L0CharacterGenerationOptionsService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/l0-characters")
@Tag(name = "L0 Character Generation", description = "L0 캐릭터 코어 생성 API")
class L0CharacterGenerationController(
    private val generationOptionsService: L0CharacterGenerationOptionsService,
) {
    @Operation(
        summary = "L0 캐릭터 생성 축 조회",
        description = "캐릭터 생성 입력 UI에서 선택할 분야, Lie, Big Five, 관계 태도, 정서 표출, 회피 방식, 말투 원형을 모두 반환한다.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "축 정보 조회 성공",
        content = [Content(schema = Schema(implementation = L0CharacterGenerationOptionsDto::class))],
    )
    @GetMapping("/generation-options")
    fun getGenerationOptions(): L0CharacterGenerationOptionsDto = generationOptionsService.getOptions()

    @Operation(
        summary = "L0 캐릭터 생성",
        description = "입력 축으로 L0 캐릭터 생성 프롬프트를 실행한다.",
    )
    @ApiResponse(
        responseCode = "202",
        description = "생성 요청을 수락함",
        content = [
            Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = L0GenerationExecuteDto::class),
            ),
        ],
    )
    @PostMapping("/generations")
    fun generate(
        @Valid @RequestBody request: L0CharacterGenerationRequest,
    ): ResponseEntity<L0GenerationExecuteDto> = ResponseEntity.accepted().build()
}
