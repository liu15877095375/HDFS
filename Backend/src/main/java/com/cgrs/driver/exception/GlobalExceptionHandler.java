package com.cgrs.driver.exception;

import com.cgrs.driver.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<String>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        
        Result<String> result = new Result<>();
        result.setCode(status.value());
        result.setMessage(message);
        
        return new ResponseEntity<>(result, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception ex) {
        Result<String> result = new Result<>();
        result.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        result.setMessage("服务器内部错误: " + ex.getMessage());
        
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
