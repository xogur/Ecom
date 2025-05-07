package com.ecommerce.project.controller;

import com.ecommerce.project.model.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CategoryController {

    // 카테고리 타입의 리스트
    private List<Category> categories = new ArrayList<>();

    // 해당 url로 get 요청이 오면 실행
    @GetMapping("/api/public/categories")
    public List<Category> getAllCategories(){

        return categories;
    }

    @PostMapping("/api/public/categories")
    public String createCategory(@RequestBody Category category){
        categories.add(category);
        return "Category added successfully";
    }
}