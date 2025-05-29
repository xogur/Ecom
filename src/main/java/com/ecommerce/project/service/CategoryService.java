package com.ecommerce.project.service;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryResponse;

import java.util.List;

public interface CategoryService {
    // 모든 카테고리를 리스트 형태로 반환하는 메서드 (조회용)
    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    // 전달된 카테고리 객체를 DB에 저장하는 메서드 (등록용)
    // 해당 함수는 String타입
    // 카테고리를 삭제하는 함수
    CategoryDTO deleteCategory(Long categoryId);

    // 카테고리 수정
    // 카테고리 타입
    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);
}