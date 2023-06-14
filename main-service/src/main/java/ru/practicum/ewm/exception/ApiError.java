package ru.practicum.ewm.exception;

import lombok.Builder;

@Builder
public class ApiError {
    private String message;
    private Throwable reason;
    private String status;
    private String timestamp;
}

