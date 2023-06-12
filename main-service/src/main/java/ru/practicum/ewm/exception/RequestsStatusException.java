package ru.practicum.ewm.exception;

public class RequestsStatusException extends RuntimeException {
    public RequestsStatusException(String message) {
        super(message);
    }
}
