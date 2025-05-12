package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
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
    private Long nextId = 1L;

    // 스프링 부트가 자동으로 CategoryRepository categoryRepository = new CategoryRepository();
    // 이런식으로 객체를 만들어 줌
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public void createCategory(Category category) {
        category.setCategoryId(nextId++);
        categoryRepository.save(category);

    }

    // 인터페이스의 deleteCategory함수를 오버라이딩
    @Override
    public String deleteCategory(Long categoryId) {

        List<Category> categories = categoryRepository.findAll();
        // stream은 categories에서 순회하겠다는 의미
        Category category = categories.stream()
                // filter는 조건
                // 람다식으로 각 카테고리 c의 카테고리 아이디를 조회해 입력받은 categoryId와 일치하는것만 추출
                .filter(c -> c.getCategoryId().equals(categoryId))
                // 그 중 첫번째 것만 가져옴
                .findFirst()
                // 예외처리 - 만약 없다면 ResponseStatusException는 스프링에서 제공하는 hppt 예외처리
                // NOT_FOUND에 맞는 상태코드를 사용자에게 반환 (404), 메시지는 Resource not found
                // 예외를 던짐
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        if (category == null)
            return "Category not found";

        categoryRepository.delete(category);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }


    @Override
    // 현재 category에는 클라이언트에서 보낸 수정할 데이터가 담겨져 있음
    public Category updateCategory(Category category, Long categoryId) {

        List<Category> categories = categoryRepository.findAll();
        // Optional은 값이 없을수도 있다는 가정을 명확하게 하기 위해 사용
        Optional<Category> optionalCategory = categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst();

        if(optionalCategory.isPresent()){
            // Optional의 get() 함수는 실제 값을 꺼냄
            Category existingCategory = optionalCategory.get();
            // 실제 값이 저장된 existingCategory의 카테고리 이름을 클라이언트에서 보낸 수정할 데이터가 담겨져 있는
            // category에서 꺼내서 적용
            existingCategory.setCategoryName(category.getCategoryName());

            // id 필드 같이 DB에서 자동으로 생성해 주는 값은
            // 저장이 된 후에 생성이 되기 때문에 변수에 save한 후의 결과 객체를 변수에 저장
            Category savedCategory = categoryRepository.save(existingCategory);
            return savedCategory;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
    }
}