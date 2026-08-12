# Character Gen 2 개발 지침

## 프로젝트 기본 원칙

- Kotlin과 Spring Boot의 기존 코드 스타일과 패키지 구조를 따른다.
- API 계층은 `ltmp.charactergen.api`, 설정은 `ltmp.charactergen.config`, JPA 모델은 `ltmp.charactergen.data` 아래에 둔다.
- 컨트롤러에는 요청 조정만 두고 프롬프트 생성, LLM 호출, 상태 전이 같은 비즈니스 로직은 서비스 계층에 둔다.
- JPA 엔티티는 기존 base entity를 재사용하고 테이블명, 컬럼명, nullable 여부와 enum 저장 방식을 명시한다.
- 변경 후 최소한 `./gradlew test`를 실행한다.

## 데이터 값의 기본값 규칙

- 데이터 전달용 DTO(`data class`)와 JPA 엔티티의 non-null 필드에는 특별한 기술적 이유가 없는 한 기본값을 선언하지 않는다.
- DTO와 엔티티의 nullable 필드는 객체 생성 편의를 위해 `= null`을 기본값으로 선언한다.
- non-null 필드는 최초 입력값이 변형 없이 끝까지 전달되게 하고, 값이 누락되거나 유효하지 않으면 임의의 값으로 대체하지 말고 오류를 발생시킨다.
- 객체를 생성하거나 함수를 호출할 때 `?: ""`, `?: 0`, 임의 enum 등 출처 없는 대체값을 인자로 넘기지 않는다. 이런 값이 필요하다면 실제로 값을 소비하는 로직에서 정책에 따라 결정하거나 오류를 발생시킨다.
- 일반 함수의 데이터 인자에도 임의의 기본값을 선언해 값의 출처를 숨기지 않는다. DTO와 엔티티의 nullable 필드에 사용하는 `= null`은 예외다.
- 프레임워크가 관리하는 ID, 감사 시각, 연관 컬렉션 등 기본 초기화가 불가피한 경우에는 그 이유가 코드에서 분명해야 한다.

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
