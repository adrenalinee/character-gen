package ltmp.charactergen.api.prompt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.service.PromptTemplateService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/v1/prompt-template-versions")
@Tag(name = "Prompt Template Versions", description = "수정과 삭제를 허용하지 않는 프롬프트 버전 API")
class PromptTemplateVersionController(
    private val promptTemplateService: PromptTemplateService,
) {
    @Operation(
        summary = "프롬프트 템플릿 버전 검색",
        description = "소속 템플릿 식별자와 버전 이름 일부로 검색한다. 조건을 생략하면 전체를 반환한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "검색 성공", content = [Content(schema = Schema(implementation = PagedModel::class))]),
            ApiResponse(responseCode = "404", description = "검색 대상으로 지정한 템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) promptTemplateId: Long?,
        @RequestParam(required = false) name: String?,
        @ParameterObject pageable: Pageable,
    ): PagedModel<PromptTemplateVersionDto> =
        PagedModel(promptTemplateService.searchVersions(promptTemplateId, name, pageable))

    @Operation(summary = "프롬프트 템플릿 버전 생성", description = "생성된 버전은 수정하거나 삭제할 수 없다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공", content = [Content(schema = Schema(implementation = PromptTemplateVersionDto::class))]),
            ApiResponse(responseCode = "400", description = "입력값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "소속 템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "동일 템플릿 내 버전 이름 중복", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreatePromptTemplateVersionRequest,
        uriBuilder: UriComponentsBuilder,
    ): ResponseEntity<PromptTemplateVersionDto> {
        val result = promptTemplateService.createVersion(request)
        val location = uriBuilder.path("/api/v1/prompt-template-versions/{id}").buildAndExpand(result.id).toUri()
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "프롬프트 템플릿 버전 단일 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = PromptTemplateVersionDto::class))]),
            ApiResponse(responseCode = "404", description = "버전 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): PromptTemplateVersionDto = promptTemplateService.getVersion(id)
}
