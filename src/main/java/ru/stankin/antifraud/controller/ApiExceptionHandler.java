package ru.stankin.antifraud.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.stankin.antifraud.dto.ApiErrorResponse;
import ru.stankin.antifraud.exception.BadRequestException;
import ru.stankin.antifraud.exception.DuplicateTransactionException;
import ru.stankin.antifraud.exception.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null ? fieldError.getDefaultMessage() : "validation failed";
        String field = fieldError != null ? fieldError.getField() : null;
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", message, field));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", exception.getMessage(), null));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateTransactionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("DUPLICATE_TRANSACTION", exception.getMessage(), "transactionId"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", exception.getMessage(), null));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", exception.getMessage(), null));
    }
}
