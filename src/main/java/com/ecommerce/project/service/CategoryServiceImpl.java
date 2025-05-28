package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.exceptions.APIException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.ecommerce.project.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;



// @Service: 이 클래스는 **스프링이 관리하는 서비스 빈(Bean)**이라는 뜻입니다.
// 컨트롤러 등에서 @Autowired로 주입받을 수 있게 됩니다.
@Service
public class CategoryServiceImpl implements CategoryService{

    // 카테고리 데이터를 담을 **리스트 (메모리 저장소)**입니다.
    // 이 리스트는 DB가 없는 상황에서 임시로 사용하는 저장소로 볼 수 있어요.
    // private List<Category> categories = new ArrayList<>();


    // 스프링 부트가 자동으로 CategoryRepository categoryRepository = new CategoryRepository();
    // 이런식으로 객체를 만들어 줌
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty())
            throw new APIException("No category created till now.");
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        Category savedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (savedCategory != null)
            throw new APIException("Category with the name " + category.getCategoryName() + " already exists !!!");
        // category.setCategoryId(nextId++);
        categoryRepository.save(category);

    }

    // 인터페이스의 deleteCategory함수를 오버라이딩
    @Override
    public String deleteCategory(Long categoryId) {

        //
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));
        if (category == null)
            return "Category not found";

        categoryRepository.delete(category);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }


    @Override
    // 현재 category에는 클라이언트에서 보낸 수정할 데이터가 담겨져 있음
    public Category updateCategory(Category category, Long categoryId) {

        // 실패했을때는 실패값을 리턴
        Category savedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));


        category.setCategoryId(categoryId);
        // 성공했을때의 리턴값을 다시 정의하기 위해 재정의
        savedCategory = categoryRepository.save(category);
        return savedCategory;
    }
}