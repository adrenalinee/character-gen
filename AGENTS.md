# Character Gen 2 개발 지침

## 프로젝트 기본 원칙

- Kotlin과 Spring Boot의 기존 코드 스타일과 패키지 구조를 따른다.
- API 계층은 `ltmp.charactergen.api`, 설정은 `ltmp.charactergen.config`, JPA 모델은 `ltmp.charactergen.data` 아래에 둔다.
- 컨트롤러에는 요청 조정만 두고 프롬프트 생성, LLM 호출, 상태 전이 같은 비즈니스 로직은 서비스 계층에 둔다.
- JPA 엔티티는 기존 base entity를 재사용하고 테이블명, 컬럼명, nullable 여부와 enum 저장 방식을 명시한다.
- 변경 후 최소한 `./gradlew test`를 실행한다.

## API와 OpenAPI 작성 규칙

- 새 API를 작성하거나 기존 API 계약을 변경할 때 OpenAPI annotation으로 스펙 설명을 반드시 함께 작성한다.
- 컨트롤러에는 API 그룹을 설명하는 `@Tag`를 작성한다.
- 각 endpoint에는 목적과 동작을 설명하는 `@Operation`을 작성한다.
- 가능한 모든 정상·오류 HTTP 상태를 `@ApiResponse` 또는 `@ApiResponses`로 작성하고, 실제 반환 상태 및 response body 타입과 일치시킨다.
- request/response DTO와 외부에 노출되는 필드에는 `@Schema`로 의미, 단위, 허용 범위, nullable 여부와 필요한 예시를 설명한다.
- Bean Validation 조건과 OpenAPI 설명이 서로 어긋나지 않게 유지한다.
- 비동기 API는 요청 수락 상태 코드와 실행 상태 DTO를 명확히 문서화한다.
- 엔티티를 API 응답으로 직접 노출하지 않고 별도의 DTO를 사용한다.
- API 변경 후 `/v3/api-docs`의 생성 여부와 스키마가 코드의 계약에 맞는지 확인한다.

## OpenAPI 노출 경로

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`
