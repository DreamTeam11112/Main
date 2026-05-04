package ru.stankin.antifraud.dto;

import java.util.List;

public record PagedResponse<T>(
        int page,
        int size,
        long totalElements,
        List<T> items
) {
}
