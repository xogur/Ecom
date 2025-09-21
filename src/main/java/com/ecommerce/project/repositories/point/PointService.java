package com.ecommerce.project.repositories.point;

import com.ecommerce.project.model.PointTransaction;
import com.ecommerce.project.model.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private static final double EARN_RATE = 0.05; // 5%

    private final PointQueryRepository pointQueryRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 현재 포인트 잔액
     */
    public long getBalance(Long userId) {
        return pointQueryRepository.getBalance(userId);
    }

    /**
     * 프리뷰 계산(주문 전)
     */
    public PreviewResult preview(Long userId, long cartTotal, long reqPointsToUse) {
        long balance = getBalance(userId);
        if (cartTotal < 0) cartTotal = 0;

        long allowedUse = Math.max(0, Math.min(reqPointsToUse, Math.min(balance, cartTotal)));
        long finalPay = cartTotal - allowedUse;
        long willEarn = Math.floorDiv(Math.round(finalPay * 100 * EARN_RATE), 100); // 정수 포인트

        return new PreviewResult(
                balance,
                allowedUse,
                finalPay,
                willEarn,
                balance - allowedUse,
                balance - allowedUse + willEarn
        );
    }

    /**
     * 주문 확정 시 포인트 반영(동일 트랜잭션 권장)
     */
    @Transactional
    public void settleAfterOrder(User user, long cartTotal, long pointsUsed, Long orderId) {
        // 프리뷰 재계산으로 방어
        PreviewResult p = preview(user.getUserId (), cartTotal, pointsUsed);

        // 1) 사용 포인트 차감
        if (p.pointsToUse > 0) {
            pointTransactionRepository.save(
                    PointTransaction.builder()
                            .user(user)
                            .type(PointTransaction.Type.USE)
                            .amount(p.pointsToUse)
                            .orderId(orderId)
                            .build()
            );
        }

        // 2) 적립 포인트 적립
        if (p.willEarn > 0) {
            pointTransactionRepository.save(
                    PointTransaction.builder()
                            .user(user)
                            .type(PointTransaction.Type.EARN)
                            .amount(p.willEarn)
                            .orderId(orderId)
                            .build()
            );
        }
    }

    // ==== DTO ====
    @lombok.Value
    public static class PreviewResult {
        long myBalanceBefore;
        long pointsToUse;
        long finalPay;
        long willEarn;
        long myBalanceAfterUse;
        long myBalanceAfterEarn;
    }
}
