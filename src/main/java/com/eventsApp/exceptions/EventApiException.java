package com.eventsApp.exceptions;

import org.springframework.http.HttpStatus;

public class EventApiException extends RuntimeException {
    private final HttpStatus status;

    public EventApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
