package com.ecommerce.project.service;

import com.ecommerce.project.payload.KakaoPayApproveResponseDto;
import com.ecommerce.project.payload.KakaoPayRequestDto;
import com.ecommerce.project.payload.KakaoPayReadyResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KakaoPayService {

    @Value("${kakao.admin-key}")
    private String adminKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, String> tidStore = new HashMap<>();


    public KakaoPayReadyResponseDto kakaoPayReady(KakaoPayRequestDto requestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("partner_order_id", "order-001");
        params.add("partner_user_id", "user-001");
        params.add("item_name", requestDto.getProductName());
        params.add("quantity", String.valueOf(requestDto.getQuantity()));
        params.add("total_amount", String.valueOf(requestDto.getTotalPrice()));
        params.add("vat_amount", "0");
        params.add("tax_free_amount", "0");

        params.add("approval_url", "http://localhost:8080/api/pay/success");
        params.add("cancel_url", "http://localhost:8080/api/pay/cancel");
        params.add("fail_url", "http://localhost:8080/api/pay/fail");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(
                "https://kapi.kakao.com/v1/payment/ready",
                request,
                KakaoPayReadyResponseDto.class
        );
    }

    public void saveTidForUser(String userId, String tid) {
        System.out.println("🟢 TID 저장됨: " + userId + " → " + tid);
        tidStore.put(userId, tid);
    }

    public String getTidForUser(String userId) {
        String tid = tidStore.get(userId);
        System.out.println("🔵 TID 조회됨: " + userId + " → " + tid);
        return tid;
    }

    public KakaoPayApproveResponseDto approvePayment(String userId, String pgToken) {
        String tid = getTidForUser(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", tid);
        params.add("partner_order_id", "order-001");
        params.add("partner_user_id", userId);
        params.add("pg_token", pgToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(
                "https://kapi.kakao.com/v1/payment/approve",
                entity,
                KakaoPayApproveResponseDto.class
        );
    }

}
