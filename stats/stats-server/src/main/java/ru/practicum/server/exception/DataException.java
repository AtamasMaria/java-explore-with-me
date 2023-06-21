package ru.practicum.server.exception;

public class DataException extends RuntimeException {
    public DataException(final String message) {
        super(message);
    }
}