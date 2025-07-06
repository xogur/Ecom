package com.ecommerce.project.payload;

import lombok.Data;

@Data
public class KakaoPayRequestDto {
    private String productName;
    private int quantity;
    private int totalPrice;
}
