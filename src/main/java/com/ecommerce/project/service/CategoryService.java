package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;

import java.util.List;

public interface CategoryService {
    // 모든 카테고리를 리스트 형태로 반환하는 메서드 (조회용)
    List<Category> getAllCategories();
    // 전달된 카테고리 객체를 DB에 저장하는 메서드 (등록용)
    void createCategory(Category category);

    String deleteCategory(Long categoryId);
}