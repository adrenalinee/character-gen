package ltmp.charactergen.api.prompt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.data.PromptTemplateType
import ltmp.charactergen.service.DefaultPromptTemplateService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/default-prompt-templates")
@Tag(
    name = "Default Prompt Templates",
    description = "용도별로 기본 선택될 프롬프트 템플릿을 조회하고 변경하는 API. " +
        "생성·대화 실행에서 프롬프트를 지정하지 않으면 여기서 지정한 템플릿의 기본 BRANCH가 실행된다.",
)
class DefaultPromptTemplateController(
    private val defaultPromptTemplateService: DefaultPromptTemplateService,
) {
    @Operation(
        summary = "기본 프롬프트 템플릿 전체 조회",
        description = "용도 타입 수만큼만 존재하는 고정 크기 목록이므로 페이징하지 않는다.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(array = ArraySchema(schema = Schema(implementation = DefaultPromptTemplateDto::class)))],
    )
    @GetMapping
    fun getAll(): List<DefaultPromptTemplateDto> = defaultPromptTemplateService.getDefaults()

    @Operation(summary = "용도별 기본 프롬프트 템플릿 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = DefaultPromptTemplateDto::class))]),
            ApiResponse(responseCode = "404", description = "해당 용도의 기본 템플릿이 지정되지 않음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{type}")
    fun get(@PathVariable type: PromptTemplateType): DefaultPromptTemplateDto =
        defaultPromptTemplateService.getDefault(type)

    @Operation(
        summary = "용도별 기본 프롬프트 템플릿 변경",
        description = "경로의 용도와 같은 type을 가진 템플릿만 기본값으로 지정할 수 있다. 지정된 적이 없으면 새로 지정한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공", content = [Content(schema = Schema(implementation = DefaultPromptTemplateDto::class))]),
            ApiResponse(responseCode = "400", description = "템플릿 용도가 경로의 type과 다름", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PutMapping("/{type}")
    fun update(
        @PathVariable type: PromptTemplateType,
        @Valid @RequestBody request: UpdateDefaultPromptTemplateRequest,
    ): DefaultPromptTemplateDto = defaultPromptTemplateService.updateDefault(type, request)
}
