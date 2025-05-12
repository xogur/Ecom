package com.ecommerce.project.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "categories")

// 카테고리 모델
public class Category {

    @Id
    // 카테고리 아이디
    private Long categoryId;
    // 카테고리 이름
    private String categoryName;

    // 카테고리 생성자/ 카테고리 아이디와, 이름 변수에 저장
    public Category(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // 게터세터
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}