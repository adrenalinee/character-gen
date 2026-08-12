package ltmp.charactergen.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Character Gen API",
        version = "v1",
        description = "대화형 AI 캐릭터의 코어와 생성 실행을 관리하는 API",
    ),
)
class OpenApiConfiguration
