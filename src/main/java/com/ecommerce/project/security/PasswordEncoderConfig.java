package com.ecommerce.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 강도 파라미터는 10~12 권장 (기본 10)
        return new BCryptPasswordEncoder();
    }
}
