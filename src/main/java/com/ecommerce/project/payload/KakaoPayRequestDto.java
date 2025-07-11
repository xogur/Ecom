package com.ecommerce.project.payload;

import lombok.Data;

@Data
public class KakaoPayRequestDto {
    private String userId;       // ✅ 사용자 정보
    private String productName;
    private int quantity;
    private int totalPrice;
    private Long addressId;
}