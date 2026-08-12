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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/v1/prompt-templates")
@Tag(name = "Prompt Templates", description = "프롬프트 템플릿 관리 API")
class PromptTemplateController(
    private val promptTemplateService: PromptTemplateService,
) {
    @Operation(summary = "프롬프트 템플릿 검색", description = "이름 일부로 템플릿을 검색한다. 이름을 생략하면 전체를 반환한다.")
    @ApiResponse(
        responseCode = "200",
        description = "검색 성공",
        content = [Content(schema = Schema(implementation = PagedModel::class))],
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @ParameterObject pageable: Pageable,
    ): PagedModel<PromptTemplateDto> = PagedModel(promptTemplateService.searchTemplates(name, pageable))

    @Operation(summary = "프롬프트 템플릿 생성")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공", content = [Content(schema = Schema(implementation = PromptTemplateDto::class))]),
            ApiResponse(responseCode = "400", description = "입력값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "동일한 이름 존재", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreatePromptTemplateRequest,
        uriBuilder: UriComponentsBuilder,
    ): ResponseEntity<PromptTemplateDto> {
        val result = promptTemplateService.createTemplate(request)
        val location = uriBuilder.path("/api/v1/prompt-templates/{id}").buildAndExpand(result.id).toUri()
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "프롬프트 템플릿 단일 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = PromptTemplateDto::class))]),
            ApiResponse(responseCode = "404", description = "템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): PromptTemplateDto = promptTemplateService.getTemplate(id)

    @Operation(summary = "프롬프트 템플릿 수정", description = "템플릿 이름을 수정한다. 버전은 수정되지 않는다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", content = [Content(schema = Schema(implementation = PromptTemplateDto::class))]),
            ApiResponse(responseCode = "400", description = "입력값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "동일한 이름 존재", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePromptTemplateRequest,
    ): PromptTemplateDto = promptTemplateService.updateTemplate(id, request)

    @Operation(summary = "프롬프트 템플릿 삭제", description = "버전이 하나도 없는 템플릿만 삭제할 수 있다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "404", description = "템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "소속 버전이 있어 삭제 불가", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        promptTemplateService.deleteTemplate(id)
        return ResponseEntity.noContent().build()
    }
}
