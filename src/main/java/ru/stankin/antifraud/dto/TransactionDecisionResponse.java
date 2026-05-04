package ru.stankin.antifraud.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransactionDecisionResponse(
        String transactionId,
        Integer riskScore,
        String decision,
        LocalDateTime processedAt,
        List<RuleTriggerResponse> triggers
) {
}
