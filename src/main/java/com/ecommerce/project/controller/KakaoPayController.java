package com.ecommerce.project.controller;

import com.ecommerce.project.payload.*;
import com.ecommerce.project.service.KakaoPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/pay")
public class KakaoPayController {

    @Autowired
    private KakaoPayService kakaoPayService;

    // 카카오페이 결제 준비 요청
    @PostMapping("/ready")
    public ResponseEntity<KakaoPayReadyResponseDto> readyToPay(@RequestBody KakaoPayRequestDto dto) {
        KakaoPayReadyResponseDto response = kakaoPayService.kakaoPayReady(dto);
        return ResponseEntity.ok(response);
    }

    // 결제 성공시 호출되는 콜백
    @GetMapping("/success")
    public ResponseEntity<?> kakaoPaySuccess(@RequestParam("pg_token") String pgToken,
                                             @RequestParam("userId") String userId) {
        try {
            KakaoPayCompleteResponseDto result = kakaoPayService.approvePaymentWithProduct(userId, pgToken);
            return ResponseEntity.ok(result); // ✅ JSON 반환
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 승인 실패: " + e.getMessage());
        }
    }

    // 사용자가 결제 취소했을 경우
    @GetMapping("/cancel")
    public ResponseEntity<String> kakaoPayCancel() {
        return ResponseEntity.ok("결제가 취소되었습니다.");
    }

    // 결제 실패시
    @GetMapping("/fail")
    public ResponseEntity<String> kakaoPayFail() {
        return ResponseEntity.ok("결제에 실패하였습니다.");
    }

    @PostMapping("/approve")
    public ResponseEntity<?> approvePayment(@RequestBody KakaoPayApproveRequestDto dto) {
        try {
            String userId = "user-001"; // 실제 로그인 사용자 ID 사용 가능
            KakaoPayApproveResponseDto response = kakaoPayService.approvePayment(userId, dto.getPgToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("결제 승인 실패: " + e.getMessage());
        }
    }
}
