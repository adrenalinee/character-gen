package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity
import ltmp.charactergen.data.VerbalTicSituationGroup
import ltmp.charactergen.data.VoiceSampleSituationGroup

/**
 * 말버릇을 만들 상황 하나. (v4 §4.1)
 *
 * `verifiedAxis`는 이 상황이 주로 어떤 축을 검증하는지이며, 프롬프트가 아니라 운영자 화면과 카탈로그 확장 판단에 쓴다.
 */
@Entity
@Table(
    name = "verbal_tic_situations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_verbal_tic_situations_situation", columnNames = ["situation"]),
    ],
)
class VerbalTicSituationEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_group", nullable = false, length = 30)
    var group: VerbalTicSituationGroup,

    @Column(nullable = false, length = 200)
    var situation: String,

    @Column(name = "verified_axis", nullable = false, length = 200)
    var verifiedAxis: String,
) : LongBaseEntity()

/**
 * 발화 샘플을 만들 상황 하나. (v4 §4.2)
 */
@Entity
@Table(
    name = "voice_sample_situations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_voice_sample_situations_situation", columnNames = ["situation"]),
    ],
)
class VoiceSampleSituationEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_group", nullable = false, length = 30)
    var group: VoiceSampleSituationGroup,

    @Column(nullable = false, length = 200)
    var situation: String,

    @Column(name = "verified_axis", nullable = false, length = 200)
    var verifiedAxis: String,
) : LongBaseEntity()
