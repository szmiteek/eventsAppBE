package com.eventsApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static java.time.LocalDateTime.now;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventApiException.class)
    public ResponseEntity<ExceptionDTO> handleEventApiException(EventApiException e) {
        ExceptionDTO dto = new ExceptionDTO(now(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(dto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ValidationErrorDTO errorDto = new ValidationErrorDTO(now());
        e.getFieldErrors().forEach(fieldError ->
                errorDto.addViolation(fieldError.getField(), fieldError.getDefaultMessage()));
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }
}
