package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.exceptions.APIException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import com.ecommerce.project.payload.CategoryDTO;
import org.modelmapper.ModelMapper;
import java.util.stream.Collectors;
import java.util.Optional;
import com.ecommerce.project.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.ecommerce.project.payload.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;



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

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize) {
        // 첫 페이지에서 5개 가져오기
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);

        List<Category> categories = categoryPage.getContent();
        if (categories.isEmpty())
            throw new APIException("No category created till now.");

        // 기존 categories의 데이터들을 각각 category이름으로  -> 옆에있는 함수들을 적용
        // category를 CategoryDTO형식으로 변환
        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category categoryFromDb = categoryRepository.findByCategoryName(category.getCategoryName());
        if (categoryFromDb != null)
            throw new APIException("Category with the name " + category.getCategoryName() + " already exists !!!");
        // category.setCategoryId(nextId++);
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);

    }

    // 인터페이스의 deleteCategory함수를 오버라이딩
    @Override
    public CategoryDTO deleteCategory(Long categoryId) {

        //
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));


        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }


    @Override
    // 현재 category에는 클라이언트에서 보낸 수정할 데이터가 담겨져 있음
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {

        // 실패했을때는 실패값을 리턴
        Category savedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));

        Category category = modelMapper.map(categoryDTO, Category.class);


        category.setCategoryId(categoryId);
        // 성공했을때의 리턴값을 다시 정의하기 위해 재정의
        savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }
}