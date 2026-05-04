package ru.stankin.antifraud.dto;

public record ApiErrorResponse(
        String errorCode,
        String message,
        String field
) {
}
