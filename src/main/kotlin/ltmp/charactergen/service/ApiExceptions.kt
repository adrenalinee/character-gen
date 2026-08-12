package ltmp.charactergen.service

class ResourceNotFoundException(val detail: String) : RuntimeException(detail)

class ResourceConflictException(val detail: String) : RuntimeException(detail)

class InvalidRequestException(val detail: String) : RuntimeException(detail)
