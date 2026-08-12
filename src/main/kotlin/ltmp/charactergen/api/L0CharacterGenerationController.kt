package ltmp.charactergen.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/l0-characters")
@Tag(name = "L0 Character Generation", description = "L0 캐릭터 코어 생성 API")
class L0CharacterGenerationController {
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
