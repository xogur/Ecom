package com.ecommerce.project.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


// JWT인증 실패시 발생하는 예외처리 코드
// 스피링이 해당 코드를 Bean에 등록
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    // 로그 기록을 위한 로거
//    로그를 찍을 대상 클래스 이름을 등록하는 코드입니다.
//        예: AuthEntryPointJwt에서 발생한 로그인지 식별 가능하도록 해줍니다.
//    로그를 출력할 때 다음과 같이 클래스명이 함께 찍히게 됩니다:
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }

}
