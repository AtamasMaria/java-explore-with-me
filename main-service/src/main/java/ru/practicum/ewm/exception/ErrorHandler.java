package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbiddenException(final ForbiddenException e) {
        log.warn("403 {}", e.getMessage());
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final MethodArgumentNotValidException e) {
        log.warn("400 {}", e.getMessage());
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(final MissingServletRequestParameterException e) {
        log.warn("400 {}", e.getMessage(), e);
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleUserNotFoundException(final NotFoundException e) {
        log.warn("404 {}", e.getMessage());
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final ValidationException e) {
        log.warn("400 {}", e.getMessage(), e);
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final BadRequestException e) {
        log.warn("400 {}", e.getMessage(), e);
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConstraintViolationException(final ConflictException e) {
        log.warn("409 {}", e.getMessage());
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("CONFLICT")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.warn("409 {}", e.getMessage());
        return ApiError.builder()
                .message(e.getMessage())
                .reason(e.toString())
                .status("CONFLICT")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable throwable) {
        log.error("Unknown error", throwable);
        return ApiError.builder()
                .message(throwable.getMessage())
                .reason(throwable.toString())
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }
}