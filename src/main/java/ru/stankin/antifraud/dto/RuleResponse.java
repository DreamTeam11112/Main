package ru.stankin.antifraud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RuleResponse(
        String ruleCode,
        String ruleName,
        Integer weight,
        BigDecimal threshold,
        Boolean enabled,
        LocalDateTime updatedAt
) {
}
