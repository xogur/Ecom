package com.ecommerce.project.payload;

import lombok.Data;

@Data
public class KakaoPayReadyResponseDto {
    private String tid;
    private String next_redirect_pc_url;
    private String created_at;
}