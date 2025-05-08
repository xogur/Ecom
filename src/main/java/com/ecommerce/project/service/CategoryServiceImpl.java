package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

// @Service: 이 클래스는 **스프링이 관리하는 서비스 빈(Bean)**이라는 뜻입니다.
// 컨트롤러 등에서 @Autowired로 주입받을 수 있게 됩니다.
@Service
public class CategoryServiceImpl implements CategoryService{

    // 카테고리 데이터를 담을 **리스트 (메모리 저장소)**입니다.
    // 이 리스트는 DB가 없는 상황에서 임시로 사용하는 저장소로 볼 수 있어요.
    private List<Category> categories = new ArrayList<>();
    private Long nextId = 1L;

    @Override
    public List<Category> getAllCategories() {
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        category.setCategoryId(nextId++);
        categories.add(category);

    }

    // 인터페이스의 deleteCategory함수를 오버라이딩
    @Override
    public String deleteCategory(Long categoryId) {
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

        categories.remove(category);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }
}