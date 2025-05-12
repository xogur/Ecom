package com.ecommerce.project.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

// @Entity 는 데이터 베이스와 해당 클래스가 매핑이 되도록 연결
@Entity(name = "categories")
// name = "categories"는 JPA 내부 이름이고,
//실제 DB 테이블 이름은 @Table(name = "테이블명")에서 따로 지정할 수 있습니다.
//만약 @Table이 없다면 기본적으로 클래스 이름이 테이블 이름이 됩니다 (category → category).
// 카테고리 모델
public class Category {

    // 기본키
    // JPA 데이터를 조회/수정 등을 처리할때 기본키를 기준으로 조회하기 때문에 필수
    @Id
    // 카테고리 아이디
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 해당 필드의 값을 자동으로 생성해줌
    private Long categoryId;
    // 카테고리 이름
    private String categoryName;

    // 카테고리 생성자/ 카테고리 아이디와, 이름 변수에 저장
    public Category(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // JPA가 데이터를 DB에서 조회할 때 생성자가 필수
    public Category() {

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