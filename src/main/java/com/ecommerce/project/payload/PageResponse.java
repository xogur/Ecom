package com.ecommerce.project.payload;

import lombok.*;
import java.util.List;

/**
 * 공통 페이징 응답 DTO
 * - Spring Data의 Page 대신, API 스펙을 고정된 형태로 내려주기 위함
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PageResponse<T> {
    private List<T> content;     // 현재 페이지 데이터
    private int pageNumber;      // 0-based
    private int pageSize;        // 요청한 페이지 크기
    private long totalElements;  // 전체 데이터 수
    private int totalPages;      // 전체 페이지 수
    private boolean lastPage;    // 마지막 페이지 여부
}
