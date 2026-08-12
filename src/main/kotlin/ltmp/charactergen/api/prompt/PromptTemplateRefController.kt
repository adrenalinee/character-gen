package ltmp.charactergen.api.prompt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import ltmp.charactergen.api.ApiErrorResponse
import ltmp.charactergen.data.PromptTemplateRefType
import ltmp.charactergen.service.PromptTemplateRefService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
@RequestMapping("/api/v1/prompt-template-refs")
@Tag(name = "Prompt Template Refs", description = "수정 가능한 BRANCH와 불변 VERSION 프롬프트 ref 관리 API")
class PromptTemplateRefController(
    private val promptTemplateRefService: PromptTemplateRefService,
) {
    @Operation(summary = "프롬프트 ref 검색", description = "템플릿, 타입, 이름, 버전 번호 조건으로 ref를 검색한다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "검색 성공", content = [Content(schema = Schema(implementation = PagedModel::class))]),
            ApiResponse(responseCode = "404", description = "검색 대상으로 지정한 템플릿 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) promptTemplateId: Long?,
        @RequestParam(required = false) type: PromptTemplateRefType?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) version: Long?,
        @ParameterObject @PageableDefault(sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PagedModel<PromptTemplateRefDto> =
        PagedModel(promptTemplateRefService.search(promptTemplateId, type, name, version, pageable))

    @Operation(summary = "빈 또는 복제 BRANCH 생성", description = "fromRefId가 없으면 빈 BRANCH, 있으면 동일 템플릿 ref의 깊은 복사본을 만든다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "BRANCH 생성 성공", content = [Content(schema = Schema(implementation = PromptTemplateRefDto::class))]),
            ApiResponse(responseCode = "400", description = "다른 템플릿 ref 복제 등 요청 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "템플릿 또는 원본 ref 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "BRANCH 이름 중복", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/branches")
    fun createBranch(
        @Valid @RequestBody request: CreatePromptTemplateBranchRequest,
        uriBuilder: UriComponentsBuilder,
    ): ResponseEntity<PromptTemplateRefDto> = created(promptTemplateRefService.createBranch(request), uriBuilder)

    @Operation(summary = "BRANCH를 VERSION으로 발행", description = "동일 템플릿 BRANCH를 복제해 다음 불변 VERSION을 만든다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "VERSION 생성 성공", content = [Content(schema = Schema(implementation = PromptTemplateRefDto::class))]),
            ApiResponse(responseCode = "400", description = "미완성 BRANCH 또는 잘못된 원본", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "템플릿 또는 BRANCH 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PostMapping("/versions")
    fun createVersion(
        @Valid @RequestBody request: CreatePromptTemplateVersionRefRequest,
        uriBuilder: UriComponentsBuilder,
    ): ResponseEntity<PromptTemplateRefDto> = created(promptTemplateRefService.createVersion(request), uriBuilder)

    @Operation(summary = "프롬프트 ref 단일 조회")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = PromptTemplateRefDto::class))]),
            ApiResponse(responseCode = "404", description = "ref 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @GetMapping("/{refId}")
    fun get(@PathVariable refId: Long): PromptTemplateRefDto = promptTemplateRefService.get(refId)

    @Operation(summary = "BRANCH 수정", description = "이름, instructions, 전체 inputs를 revision 기반으로 수정한다. VERSION은 수정할 수 없다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공", content = [Content(schema = Schema(implementation = PromptTemplateRefDto::class))]),
            ApiResponse(responseCode = "400", description = "입력값 오류", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "ref 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "VERSION 수정, 이름 중복 또는 revision 충돌", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @PutMapping("/{refId}/branch")
    fun updateBranch(
        @PathVariable refId: Long,
        @Valid @RequestBody request: UpdatePromptTemplateBranchRequest,
    ): PromptTemplateRefDto = promptTemplateRefService.updateBranch(refId, request)

    @Operation(summary = "프롬프트 ref 삭제", description = "다른 ref 또는 실행 이력이 참조하지 않는 BRANCH와 VERSION을 삭제한다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "404", description = "ref 없음", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "다른 데이터가 참조해 삭제 불가", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @DeleteMapping("/{refId}")
    fun delete(@PathVariable refId: Long): ResponseEntity<Void> {
        promptTemplateRefService.delete(refId)
        return ResponseEntity.noContent().build()
    }

    private fun created(result: PromptTemplateRefDto, uriBuilder: UriComponentsBuilder): ResponseEntity<PromptTemplateRefDto> {
        val location = uriBuilder.path("/api/v1/prompt-template-refs/{refId}").buildAndExpand(result.id).toUri()
        return ResponseEntity.created(location).body(result)
    }
}
