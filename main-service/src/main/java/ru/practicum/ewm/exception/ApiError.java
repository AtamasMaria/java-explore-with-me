package ru.practicum.ewm.exception;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiError {
    private String message;
    private String reason;
    private String status;
    private String timestamp;
}

