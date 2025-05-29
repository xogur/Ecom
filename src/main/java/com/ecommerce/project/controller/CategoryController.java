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
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
//
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

    // http://localhost:8080/echo?message=안녕
    // 이런 식으로 쿼리스트링 형식으로 전달할 메시지를 받음
    // required = false는 message가 없어도 에러가 안 나도록 해주는 옵션


    @GetMapping("/public/categories")
    // http 상태코드를 설정하기 위해 ResponseEntity 를 사용
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(name = "pageNumber") Integer pageNumber,
            @RequestParam(name = "pageSize") Integer pageSize) {
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber, pageSize);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    // http 상태코드를 설정하기 위해 ResponseEntity 를 사용
    // 페이로드의 데이터를 받기 위해 @RequestBody를 사용
    // @Valid 는 @RequestBody로 들어온 Category 객체의 필드들에 대해,notnull등을 감지하고 검증해 준다
    // 만약 @Valid가 없으면 500으로 서버 에러가 발생하는 반면 @Valid를 추가하면 해당 필드의 형식에 맞는지 검증을 해주기 때문에 틀리면 400에러가 발생함
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO){
        CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }

    // 카테고리 삭제하는 API추가
    @DeleteMapping("/admin/categories/{categoryId}")
    // @Path Variable은 url에 {categoryId}이런 형식으로 경로에서 categoryId를 꺼내오는 방식
    // ResponseEntity<T>는 본문(Body)뿐만 아니라 HTTP 상태 코드도 함께 설정 가능
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId){
        CategoryDTO deletedCategory = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(deletedCategory, HttpStatus.OK);


    }

    // 클라이언트에서 /api/public/categories/{categoryId} 요청이 오면 실행
    // put 메소드를 사용
    @PutMapping("/public/categories/{categoryId}")
    // 페이로드에 담긴 데이터를 category에 담고, {categoryId}를 categoryId를 담음
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO,
                                                 @PathVariable Long categoryId){

            // updateCategory함수를 실행
        CategoryDTO savedCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.OK);

    }
}