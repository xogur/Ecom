package com.ecommerce.project.repositories.point;

import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PointController {

    private final PointService pointService;

    @GetMapping("/points/balance")
    public BalanceRes balance(@AuthenticationPrincipal UserDetailsImpl me) {
        long bal = pointService.getBalance(me.getId());
        return new BalanceRes(bal);
    }

    @PostMapping ("/orders/preview")
    public PreviewRes preview(@AuthenticationPrincipal UserDetailsImpl me,
                              @RequestBody PreviewReq req) {
        var p = pointService.preview(me.getId(), req.cartTotal(), req.pointsToUse());
        return new PreviewRes(
                p.getMyBalanceBefore(),
                p.getPointsToUse(),
                p.getFinalPay(),
                p.getWillEarn(),
                p.getMyBalanceAfterUse(),
                p.getMyBalanceAfterEarn()
        );
    }

    // === DTOs ===
    public record PreviewReq(@Min (0) long cartTotal, @Min(0) long pointsToUse) {}
    public record BalanceRes(long balance) {}
    public record PreviewRes(long myBalanceBefore,
                             long pointsToUse,
                             long finalPay,
                             long willEarn,
                             long myBalanceAfterUse,
                             long myBalanceAfterEarn) {}
}
