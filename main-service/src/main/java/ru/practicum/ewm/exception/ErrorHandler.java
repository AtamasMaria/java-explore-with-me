package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleBadRequestException(final MethodArgumentNotValidException e) {
        log.warn("400 {}", e.getMessage());
        return ErrorMessage.builder().error(e.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage handleUserNotFoundException(final NotFoundException e) {
        log.warn("404 {}", e.getMessage());
        return ErrorMessage.builder().error(e.getMessage()).build();
    }


    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleValidationException(ValidationException e) {
        log.warn("400 {}", e.getMessage(), e);
        return ErrorMessage.builder().error(e.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage handleMissingServletRequestParameterException(final MissingServletRequestParameterException e) {
        log.warn("400 {}", e.getMessage(), e);
        return ErrorMessage.builder().error(e.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessage handleConstraintViolationException(final ConflictException e) {
        log.warn("409 {}", e.getMessage());
        return ErrorMessage.builder().error(e.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorMessage handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("409 {}", e.getMessage());
        return ErrorMessage.builder().error(e.getMessage()).build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessage handleThrowable(Throwable throwable) {
        log.error("Unknown error", throwable);
        return ErrorMessage.builder().error(throwable.getMessage()).build();
    }
}