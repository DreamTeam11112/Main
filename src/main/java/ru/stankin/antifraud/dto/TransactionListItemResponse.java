package ru.stankin.antifraud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionListItemResponse(
        String transactionId,
        String customerId,
        BigDecimal amount,
        String currency,
        LocalDateTime timestamp,
        String decision,
        Integer riskScore
) {
}
