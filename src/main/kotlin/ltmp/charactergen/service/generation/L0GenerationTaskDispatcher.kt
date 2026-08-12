package ltmp.charactergen.service.generation

import jakarta.annotation.PreDestroy
import ltmp.charactergen.service.GenerationCapacityExceededException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@Component
class L0GenerationTaskDispatcher(
    @Value("\${openai.generation.max-concurrency}") maxConcurrency: Int,
    private val generationExecutor: L0GenerationExecutor,
) {
    private val permits: Semaphore
    private val executorService: ExecutorService

    init {
        require(maxConcurrency > 0) { "openai.generation.max-concurrency must be greater than zero" }
        permits = Semaphore(maxConcurrency, true)
        executorService = Executors.newFixedThreadPool(
            maxConcurrency,
            Thread.ofPlatform().name("l0-generation-", 1).factory(),
        )
    }

    /**
     * 현재 트랜잭션에서 실행 용량을 선점하고, 커밋된 뒤에만 비동기 작업을 제출한다.
     * 트랜잭션이 롤백되면 선점한 용량도 즉시 반환한다.
     */
    fun dispatchAfterCommit(generationId: Long) {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Generation dispatch requires an active transaction"
        }
        check(TransactionSynchronizationManager.isSynchronizationActive()) {
            "Generation dispatch requires transaction synchronization"
        }
        if (!permits.tryAcquire()) {
            throw GenerationCapacityExceededException("동시에 실행할 수 있는 캐릭터 생성 작업 수를 초과했습니다")
        }

        var submitted = false
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    submitted = true
                    try {
                        executorService.execute {
                            try {
                                generationExecutor.execute(generationId)
                            } finally {
                                permits.release()
                            }
                        }
                    } catch (failure: RuntimeException) {
                        permits.release()
                        generationExecutor.failBeforeExecution(generationId, failure)
                    }
                }

                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED && !submitted) {
                        permits.release()
                    }
                }
            },
        )
    }

    @PreDestroy
    fun shutdown() {
        executorService.shutdown()
    }
}
