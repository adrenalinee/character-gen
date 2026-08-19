package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.BigFiveVector
import ltmp.charactergen.data.LongBaseEntity

@Entity
@Table(
    name = "attachment_styles",
    uniqueConstraints = [UniqueConstraint(name = "uk_attachment_styles_type", columnNames = ["type"])],
)
class AttachmentStyleEntity(
    @Column(nullable = false, length = 100)
    var type: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @Embedded
    var bigFiveVector: BigFiveVector,
) : LongBaseEntity()
