package com.ecommerce.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 스프링에게 이 함수는 설정함수라서 스프링이 기억하도록 설정
@Configuration
public class AppConfig {

    @Bean
    // Bean은 나중에 꺼내쓰기 쉽도록 스프링에 저장해두라고 하는 어노테이션
    public ModelMapper modelMapper(){
        // 데이터를 다른 형식으로 바꿔주는 똑똑한 변환기 == ModelMapper
        return new ModelMapper();
    }
}