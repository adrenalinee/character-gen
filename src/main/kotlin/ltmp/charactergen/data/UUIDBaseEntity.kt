package ltmp.charactergen.data

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class UUIDBaseEntity(
    id: UUID?,
): UUIDPrimaryKeyEntity(id) {
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
