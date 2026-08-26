package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.Table
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.data.RelationshipStageLevel

/**
 * 관계 깊이 한 단계에서의 거리·호칭·감정 범위·회피 강도. 서버가 현재 단계에 해당하는 한 행만 대화에 주입한다.
 */
@Entity
@Table(name = "character_relationship_stages")
class CharacterRelationshipStageEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_level", nullable = false, length = 20)
    val level: RelationshipStageLevel,

    @Lob
    @Column(nullable = false)
    val attachment: String,

    @Lob
    @Column(nullable = false)
    val address: String,

    @Lob
    @Column(name = "emotional_range", nullable = false)
    val emotionalRange: String,

    @Lob
    @Column(nullable = false)
    val avoidance: String,
) : LongBaseEntity()
