package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "point_transactions",
        indexes = {
                @Index(name = "idx_point_tx_user_created", columnList = "user_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    public enum Type {
        EARN,   // 적립 (+)
        USE     // 사용 (-)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_point_tx_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private Type type;

    // 절대값 포인트(+)로 저장. 부호는 type으로 구분
    @Column(name = "amount", nullable = false)
    private Long amount;

    // 어떤 주문과 연관된 포인트인지(선택)
    @Column(name = "order_id")
    private Long orderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
