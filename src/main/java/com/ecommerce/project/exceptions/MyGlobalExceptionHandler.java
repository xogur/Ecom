package com.ecommerce.project.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
// 모든 @RestController에서 발생하는 예외를 전역(Global) 으로 가로채 처리할 수 있게 합니다.
public class MyGlobalExceptionHandler {

    // 예외 헨들러
    // 사용자의 요청 객체에서 유효성 검사(@Valid)에서 오류가 발생하면 동작
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 키-밸류 형식으로 각각 타입은 스트링
    // 던져진 예외를 e에 담음
    public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e){
        // 키-밸류를 저장할 변수를 선언
        Map<String, String> response = new HashMap<>();
        // MethodArgumentNotValidException이 발생하면, 어떤 필드가 잘못됐는지에 대한 상세 정보를 담고 있는 객체입니다.
        // getAllErrors()는 getBindingResult()로 가져온 정보에서 오류 객체를 가져온다.
        // err는 각 오류 객체
        // getDefaultMessage는 검증에 실패한 필드에 대해 설정된 메시지를 가져옵니다.
        e.getBindingResult().getAllErrors().forEach(err -> {
            // err는 ObjectError타입인데 FieldError타입으로 변경해서 getField()함수를 사용
            String fieldName = ((FieldError)err).getField();
            String message = err.getDefaultMessage();
            // 선언해둔 response 변수에 키는 필드이름, 값은 메시지를 담음
            response.put(fieldName,message);
        });

        return new ResponseEntity<Map<String,String>>(response,
                HttpStatus.BAD_REQUEST);
    }
}