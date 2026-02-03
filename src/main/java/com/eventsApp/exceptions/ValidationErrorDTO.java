package com.eventsApp.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationErrorDTO extends ExceptionDTO{

    private static final String MESSAGE = "Validation errors";

    private final List<ViolationInfo> violations = new ArrayList<>();

    public ValidationErrorDTO(LocalDateTime timestamp) {
        super(timestamp, MESSAGE);
    }

    public void addViolation(String field, String message) {
        violations.add(new ViolationInfo(field,message));
    }

    @Getter
    @AllArgsConstructor
    private static class ViolationInfo {
        private String field;
        private String message;
    }
}