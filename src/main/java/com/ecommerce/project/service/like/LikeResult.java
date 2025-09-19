package com.ecommerce.project.service.like;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeResult {
    /** 이번 클릭 후 상태 (true=좋아요됨, false=취소됨) */
    private boolean liked;
    /** 현재 상품의 총 좋아요 수 */
    private long count;
}