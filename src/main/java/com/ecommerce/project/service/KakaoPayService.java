package com.ecommerce.project.service;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private AuthUtil authUtil;

    @Value("${kakao.admin-key}")
    private String adminKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, String> tidStore = new HashMap<>();

    private final Map<String, KakaoPayRequestDto> productStore = new HashMap<>();


    public KakaoPayReadyResponseDto kakaoPayReady(KakaoPayRequestDto requestDto) {

        User user = userRepository.findById(Long.valueOf(requestDto.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + requestDto.getUserId()));

        String userEmail = user.getEmail();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("partner_order_id", "order-001");
        params.add("partner_user_id", userEmail);
        params.add("item_name", requestDto.getProductName());
        params.add("quantity", String.valueOf(requestDto.getQuantity()));
        params.add("total_amount", String.valueOf(requestDto.getTotalPrice()));
        params.add("vat_amount", "0");
        params.add("tax_free_amount", "0");

        params.add("approval_url", frontendUrl + "payment/success?userId=" + requestDto.getUserId());
        params.add("cancel_url", "http://localhost:8080/api/pay/cancel");
        params.add("fail_url", "http://localhost:8080/api/pay/fail");

        System.out.println ("totalPrice = " + requestDto.getTotalPrice());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        KakaoPayReadyResponseDto response = restTemplate.postForObject(
                "https://kapi.kakao.com/v1/payment/ready",
                request,
                KakaoPayReadyResponseDto.class
        );

        tidStore.put(requestDto.getUserId(), response.getTid());
        productStore.put(requestDto.getUserId(), requestDto);
        return response;
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

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        String userEmail = user.getEmail();

        String tid = getTidForUser(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", tid);
        params.add("partner_order_id", "order-001");
        params.add("partner_user_id", userEmail);
        params.add("pg_token", pgToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        return restTemplate.postForObject(
                "https://kapi.kakao.com/v1/payment/approve",
                entity,
                KakaoPayApproveResponseDto.class
        );
    }

    public KakaoPayCompleteResponseDto approvePaymentWithProduct(String userId, String pgToken) {
        String tid = tidStore.get(userId);
        KakaoPayRequestDto product = productStore.get(userId);

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        String userEmail = user.getEmail();

        // 1. 카카오 승인 요청
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", tid);
        params.add("partner_order_id", "order-001");
        params.add("partner_user_id", userEmail);
        params.add("pg_token", pgToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        KakaoPayApproveResponseDto kakaoRes = restTemplate.postForObject(
                "https://kapi.kakao.com/v1/payment/approve",
                entity,
                KakaoPayApproveResponseDto.class
        );

        // 2. 주문 생성 (DB 저장)
        OrderRequestDTO orderRequestDTO = new OrderRequestDTO();
        orderRequestDTO.setAddressId(product.getAddressId());
        orderRequestDTO.setPgName("KakaoPay");
        orderRequestDTO.setPgPaymentId(kakaoRes.getTid());
        orderRequestDTO.setPgStatus("succeeded");
        orderRequestDTO.setPgResponseMessage("카카오페이 결제 승인 완료");

         // 또는 authUtil.loggedInEmail();
        OrderDTO order = orderService.placeOrder(
                userEmail, // ✅ 이메일 전달
                orderRequestDTO.getAddressId(),
                "KakaoPay",
                orderRequestDTO.getPgName(),
                orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(),
                orderRequestDTO.getPgResponseMessage()
        );

        AddressDTO addressDTO = addressService.getAddressesById(orderRequestDTO.getAddressId());
        order.setAddress(addressDTO);

        // 3. 결제 + 주문 데이터 묶어서 응답
        KakaoPayCompleteResponseDto result = new KakaoPayCompleteResponseDto();
        result.setKakaoResponse(kakaoRes);
        result.setProductName(product.getProductName());
        result.setQuantity(product.getQuantity());
        result.setTotalPrice(product.getTotalPrice());
        result.setOrder(order); // ✅ 주문 정보 포함

        return result;
    }

}
