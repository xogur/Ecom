package com.ecommerce.project.payload;

import lombok.Data;

@Data
public class KakaoPayCompleteResponseDto {
    private KakaoPayApproveResponseDto kakaoResponse;
    private String productName;
    private int quantity;
    private int totalPrice;

    private OrderDTO order;
}