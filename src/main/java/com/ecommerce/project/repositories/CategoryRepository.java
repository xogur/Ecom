package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;


// Spring이 자동으로 저장/조회/삭제/수정 기능을 만들어주기 때문에, 우리는 그냥 사용만 하면 돼요.
// JpaRepository를 상속함으로써 가능
public interface CategoryRepository extends JpaRepository<Category,Long> {
}