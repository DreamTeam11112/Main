package ru.stankin.antifraud.service;

import java.math.BigDecimal;
import java.util.List;
import ru.stankin.antifraud.entity.TransactionEntity;

public record CustomerHistoryStats(
        List<TransactionEntity> history,
        long historicalTransactionCount,
        BigDecimal meanAmount,
        BigDecimal standardDeviationAmount,
        long transactionsLastHour,
        long transactionsLast24Hours
) {
}
