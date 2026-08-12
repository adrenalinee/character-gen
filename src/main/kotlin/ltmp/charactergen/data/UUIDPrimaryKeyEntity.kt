package ltmp.charactergen.data

import com.fasterxml.uuid.Generators
import jakarta.persistence.*
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.domain.Persistable
import java.util.*
import kotlin.jvm.Transient

/**
 * from: https://spoqa.github.io/2022/08/16/kotlin-jpa-entity.html
 */
@MappedSuperclass
abstract class UUIDPrimaryKeyEntity(
    id: UUID?,
) : Persistable<UUID> {
    @Id
    @Column(columnDefinition = "uuid")
    private val id: UUID = id ?: Generators.timeBasedGenerator().generate()

    // Spring Data Persistable의 저장 생명주기를 추적하기 위한 내부 상태다.
    @Transient
    private var _isNew = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = _isNew

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (other !is HibernateProxy && this::class != other::class) {
            return false
        }

        return id == getIdentifier(other)
    }

    private fun getIdentifier(obj: Any): Any? {//Serializable {
        return if (obj is HibernateProxy) {
            obj.hibernateLazyInitializer.identifier
        } else {
            (obj as UUIDPrimaryKeyEntity).id
        }
    }

    override fun hashCode() = Objects.hashCode(id)

    @PostPersist
    @PostLoad
    protected fun load() {
        _isNew = false
    }
}
