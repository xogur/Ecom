package com.ecommerce.project.payload;

import lombok.Data;

@Data
public class KakaoPayApproveRequestDto {
    private String pgToken;
    private String userId;
}