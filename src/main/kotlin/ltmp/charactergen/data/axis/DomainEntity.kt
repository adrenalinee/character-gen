package ltmp.charactergen.data.axis

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "character_domains",
    uniqueConstraints = [UniqueConstraint(name = "uk_character_domains_name", columnNames = ["name"])],
)
class DomainEntity(
    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(nullable = false, length = 1000)
    var description: String = "",
) : LongBaseEntity()
