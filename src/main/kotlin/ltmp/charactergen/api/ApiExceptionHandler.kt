package ltmp.charactergen.api

import ltmp.charactergen.service.ResourceConflictException
import ltmp.charactergen.service.ResourceNotFoundException
import ltmp.charactergen.service.InvalidRequestException
import ltmp.charactergen.service.GenerationCapacityExceededException
import ltmp.charactergen.service.ChatSessionCapacityExceededException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(exception: InvalidRequestException): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.detail)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(exception: ResourceNotFoundException): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.detail)

    @ExceptionHandler(ResourceConflictException::class)
    fun handleConflict(exception: ResourceConflictException): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", exception.detail)

    @ExceptionHandler(GenerationCapacityExceededException::class)
    fun handleGenerationCapacityExceeded(
        exception: GenerationCapacityExceededException,
    ): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.TOO_MANY_REQUESTS, "GENERATION_CAPACITY_EXCEEDED", exception.detail)

    @ExceptionHandler(ChatSessionCapacityExceededException::class)
    fun handleChatSessionCapacityExceeded(
        exception: ChatSessionCapacityExceededException,
    ): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.TOO_MANY_REQUESTS, "CHAT_SESSION_CAPACITY_EXCEEDED", exception.detail)

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(exception: ObjectOptimisticLockingFailureException): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", exception.message.toString())

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(exception: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> =
        error(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", exception.mostSpecificCause.toString())

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = exception.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message)
    }

    private fun error(status: HttpStatus, code: String, message: String): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                code = code,
                message = message,
                timestamp = OffsetDateTime.now(),
            ),
        )
}
