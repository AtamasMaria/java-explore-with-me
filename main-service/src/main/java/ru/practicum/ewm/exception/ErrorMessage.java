package ru.practicum.ewm.exception;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorMessage {
    private String error;
}
