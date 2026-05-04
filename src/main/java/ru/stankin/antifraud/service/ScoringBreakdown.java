package ru.stankin.antifraud.service;

import java.math.BigDecimal;

public record ScoringBreakdown(
        BigDecimal zScore,
        BigDecimal riskCoefficient,
        int unusualnessIndex,
        boolean nightTransaction,
        boolean newDevice,
        boolean newCountry
) {
}
