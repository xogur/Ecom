package com.ecommerce.project.exceptions;

public class APIException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public APIException() {
    }

    public APIException(String message) {
        // 부모 클래스인 RuntimeException의 생성자를 message 파라미터를 담아 호출
        super(message);
    }
}