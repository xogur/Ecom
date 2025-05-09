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
@RequestMapping("/api")
public class CategoryController {

    // @Autowired: 스프링이 CategoryService 객체를 자동으로 주입(Inject) 합니다.
    // 즉, new CategoryService() 하지 않아도 스프링이 자동으로 만들어서 넣어줌
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/public/categories")
    // http 상태코드를 설정하기 위해 ResponseEntity 를 사용
    public ResponseEntity<List<Category>> getAllCategories(){
        List<Category> categories = categoryService.getAllCategories();
        // 조회된 categories와 상태코드 200을 반환
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    // http 상태코드를 설정하기 위해 ResponseEntity 를 사용
    // 페이로드의 데이터를 받기 위해 @RequestBody를 사용
    public ResponseEntity<String> createCategory(@RequestBody Category category){
        categoryService.createCategory(category);
        // "Category added successfully"를 클라이언트에 반환하고 상태코드 201반환
        return new ResponseEntity<>("Category added successfully", HttpStatus.CREATED);
    }

    // 카테고리 삭제하는 API추가
    @DeleteMapping("/admin/categories/{categoryId}")
    // @Path Variable은 url에 {categoryId}이런 형식으로 경로에서 categoryId를 꺼내오는 방식
    // ResponseEntity<T>는 본문(Body)뿐만 아니라 HTTP 상태 코드도 함께 설정 가능
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId){
        try {
            // deleteCategory함수를 실행
            // status를 반환하고 상태코드는 200
            String status = categoryService.deleteCategory(categoryId);
            // return new ResponseEntity<>(status, HttpStatus.OK);

            // 상태코드 200을 반환하고 페이로드에 status를 담음
            return ResponseEntity.status(HttpStatus.OK).body(status);

            // 예외처리
        } catch (ResponseStatusException e){
            // 예외가 던진 메시지, 상태코드
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }

    // 클라이언트에서 /api/public/categories/{categoryId} 요청이 오면 실행
    // put 메소드를 사용
    @PutMapping("/public/categories/{categoryId}")
    // 페이로드에 담긴 데이터를 category에 담고, {categoryId}를 categoryId를 담음
    public ResponseEntity<String> updateCategory(@RequestBody Category category,
                                                 @PathVariable Long categoryId){
        try{
            // updateCategory함수를 실행
            Category savedCategory = categoryService.updateCategory(category, categoryId);
            return new ResponseEntity<>("Category with category id: " + categoryId, HttpStatus.OK);
        } catch (ResponseStatusException e){
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }
}