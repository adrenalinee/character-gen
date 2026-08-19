package ltmp.charactergen.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ltmp.charactergen.data.LongBaseEntity

/**
 * 같은 상황에서 정반대를 지시해 함께 쓸 수 없는 축 조합.
 *
 * 표현 축 네 개는 원칙적으로 서로 다른 상황에 적용되므로 충돌하지 않는다.
 * 예외는 "감정적인 화제가 나왔을 때"에서 겹치는 정서 표출과 회피 방식뿐이라
 * 이 조합만 매트릭스로 관리한다.
 */
@Entity
@Table(
    name = "axis_conflicts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_axis_conflicts_pair",
            columnNames = ["emotional_expression_id", "avoidance_style_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_axis_conflicts_emotional_expression_id", columnList = "emotional_expression_id"),
        Index(name = "idx_axis_conflicts_avoidance_style_id", columnList = "avoidance_style_id"),
    ],
)
class AxisConflictEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotional_expression_id", nullable = false)
    var emotionalExpression: EmotionalExpressionEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avoidance_style_id", nullable = false)
    var avoidanceStyle: AvoidanceStyleEntity,

    @Column(nullable = false, length = 500)
    var reason: String,
) : LongBaseEntity()
