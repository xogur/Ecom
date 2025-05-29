package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// data는 롬복으로 게터세터를 자동으로 제공해 주고 추가적인 효과도 있음
@Data

// 두 어노테이션은 파라미터가 있는 생성자든 어떤 파라미터가 있던 생성자든 자동으로 매핑해줌
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long categoryId;
    private String categoryName;
}