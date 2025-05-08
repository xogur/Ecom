package com.ecommerce.project.controller;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;







import java.util.ArrayList;
import java.util.List;

// @RestController는 http요청을 처리하는 메소드 임을 나타냄
// 내부 메서드가 반환하는 객체는 자동으로 JSON 형태로 응답됩니다.
// 즉, @ResponseBody가 자동으로 붙음
@RestController
public class CategoryController {

    // @Autowired: 스프링이 CategoryService 객체를 자동으로 주입(Inject) 합니다.
    // 즉, new CategoryService() 하지 않아도 스프링이 자동으로 만들어서 넣어줌
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/api/public/categories")
    public ResponseEntity<List<Category>> getAllCategories(){
        List<Category> categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @PostMapping("/api/public/categories")
    public ResponseEntity<String> createCategory(@RequestBody Category category){
        categoryService.createCategory(category);
        return new ResponseEntity<>("Category added successfully", HttpStatus.CREATED);
    }

    // 카테고리 삭제하는 API추가
    @DeleteMapping("/api/admin/categories/{categoryId}")
    // @Path Variable은 url에 {categoryId}이런 형식으로 경로에서 categoryId를 꺼내오는 방식
    // ResponseEntity<T>는 본문(Body)뿐만 아니라 HTTP 상태 코드도 함께 설정 가능
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId){
        try {
            // deleteCategory함수를 실행
            // status를 반환하고 상태코드는 200
            String status = categoryService.deleteCategory(categoryId);
            // return new ResponseEntity<>(status, HttpStatus.OK);
            return ResponseEntity.status(HttpStatus.OK).body(status);

            // 예외처리
        } catch (ResponseStatusException e){
            // 예외가 던진 메시지, 상태코드
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }
}