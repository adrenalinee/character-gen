package ltmp.charactergen.data

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class LongBaseEntity {
    // IDENTITY가 저장 시 실제 ID로 교체하기 전 사용하는 JPA primitive sentinel이다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: OffsetDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime

    @PrePersist
    protected fun initializeAuditTimestamps() {
        val now = OffsetDateTime.now()
        if (!this::createdAt.isInitialized) {
            createdAt = now
        }
        if (!this::updatedAt.isInitialized) {
            updatedAt = now
        }
    }

    @PreUpdate
    protected fun updateAuditTimestamp() {
        updatedAt = OffsetDateTime.now()
    }
}
