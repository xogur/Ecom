package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCardDTO {
    private Long productId;
    private String productName;
    private String image;
    private Double price;
    private Double specialPrice;
    private Long likeCount; // Redis 카운트
}
