package ru.practicum.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessage handleThrowable(final Throwable e) {
        log.warn("Unknown error", e);
        return ErrorMessage.builder().error(e.getMessage()).build();
    }
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleDateExceptional(final DataException e) {
        log.warn("Ошибка времени", e);
        return ErrorMessage.builder().error(e.getMessage()).build();
    }
}
