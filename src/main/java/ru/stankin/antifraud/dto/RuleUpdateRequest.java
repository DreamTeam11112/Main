package ru.stankin.antifraud.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RuleUpdateRequest(
        @NotBlank String ruleCode,
        @NotNull @Min(0) Integer weight,
        BigDecimal threshold,
        @NotNull Boolean enabled
) {
}
