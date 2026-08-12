package ltmp.charactergen.service.generation

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "openai.generation",
    name = ["scheduler-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class L0GenerationScheduler(
    private val executor: L0GenerationExecutor,
) {
    @Scheduled(
        fixedDelayString = "\${openai.generation.scheduler-fixed-delay}",
        initialDelayString = "\${openai.generation.scheduler-initial-delay}",
    )
    fun executeNextGeneration() {
        executor.executeNext()
    }
}
