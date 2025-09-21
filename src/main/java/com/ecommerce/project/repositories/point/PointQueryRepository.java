package com.ecommerce.project.repositories.point;

import com.ecommerce.project.model.QPointTransaction;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointQueryRepository {

    private final JPAQueryFactory query;

    /**
     * 현재 잔액 = EARN 합 - USE 합
     */
    public long getBalance(Long userId) {
        QPointTransaction tx = QPointTransaction.pointTransaction;

        Long earn = query.select(tx.amount.sum())
                .from(tx)
                .where(tx.user.userId.eq(userId)
                        .and(tx.type.eq(com.ecommerce.project.model.PointTransaction.Type.EARN)))
                .fetchOne();

        Long use = query.select(tx.amount.sum())
                .from(tx)
                .where(tx.user.userId.eq(userId)
                        .and(tx.type.eq(com.ecommerce.project.model.PointTransaction.Type.USE)))
                .fetchOne();

        long e = earn == null ? 0L : earn;
        long u = use == null ? 0L : use;
        return e - u;
    }
}
