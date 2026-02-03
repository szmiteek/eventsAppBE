package com.eventsApp.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
public class ExceptionDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime timestamp;
    private final String message;
}
