package ru.stankin.antifraud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.stankin.antifraud.domain.Decision;
import ru.stankin.antifraud.dto.TransactionCheckRequest;
import ru.stankin.antifraud.entity.CustomerProfileEntity;
import ru.stankin.antifraud.entity.RuleConfigEntity;

@Component
public class FraudRuleEngine {

    public FraudEvaluationResult evaluate(
            TransactionCheckRequest request,
            CustomerProfileEntity profile,
            CustomerHistoryStats historyStats,
            List<RuleConfigEntity> rules
    ) {
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        int riskScore = 0;
        boolean newDevice = isNewDevice(request, profile);
        boolean newCountry = isNewCountry(request, profile);
        boolean nightTransaction = isNightTransaction(request);
        BigDecimal zScore = calculateZScore(request, historyStats);
        BigDecimal riskCoefficient = calculateRiskCoefficient(request, historyStats, zScore, newDevice, newCountry, nightTransaction);
        int unusualnessIndex = calculateUnusualnessIndex(request, historyStats, zScore, newDevice, newCountry, nightTransaction);

        for (RuleConfigEntity rule : rules) {
            if (Boolean.FALSE.equals(rule.getEnabled())) {
                continue;
            }

            if (isTriggered(rule, request, profile, historyStats, zScore, riskCoefficient, unusualnessIndex, nightTransaction, newDevice, newCountry)) {
                riskScore += rule.getWeight();
                triggeredRules.add(new TriggeredRule(
                        rule.getRuleCode(),
                        rule.getRuleName(),
                        rule.getWeight(),
                        buildDetails(rule, request, profile, historyStats, zScore, riskCoefficient, unusualnessIndex, nightTransaction)
                ));
            }
        }

        return new FraudEvaluationResult(
                riskScore,
                toDecision(riskScore),
                triggeredRules,
                new ScoringBreakdown(zScore, riskCoefficient, unusualnessIndex, nightTransaction, newDevice, newCountry)
        );
    }

    private boolean isTriggered(
            RuleConfigEntity rule,
            TransactionCheckRequest request,
            CustomerProfileEntity profile,
            CustomerHistoryStats historyStats,
            BigDecimal zScore,
            BigDecimal riskCoefficient,
            int unusualnessIndex,
            boolean nightTransaction,
            boolean newDevice,
            boolean newCountry
    ) {
        return switch (rule.getRuleCode()) {
            case "HIGH_AMOUNT" -> compareAmount(request.amount(), rule.getThresholdValue()) >= 0;
            case "VERY_HIGH_AMOUNT" -> compareAmount(request.amount(), rule.getThresholdValue()) >= 0;
            case "NEW_DEVICE" -> newDevice;
            case "NEW_COUNTRY" -> newCountry;
            case "NIGHT_TRANSACTION" -> nightTransaction;
            case "VELOCITY_24H" -> isVelocityTriggered(rule, historyStats);
            case "HIGH_Z_SCORE" -> rule.getThresholdValue() != null && zScore.compareTo(rule.getThresholdValue()) >= 0;
            case "HIGH_RISK_COEFFICIENT" -> rule.getThresholdValue() != null && riskCoefficient.compareTo(rule.getThresholdValue()) >= 0;
            case "HIGH_UNUSUALNESS" -> rule.getThresholdValue() != null
                    && BigDecimal.valueOf(unusualnessIndex).compareTo(rule.getThresholdValue()) >= 0;
            default -> false;
        };
    }

    private String buildDetails(
            RuleConfigEntity rule,
            TransactionCheckRequest request,
            CustomerProfileEntity profile,
            CustomerHistoryStats historyStats,
            BigDecimal zScore,
            BigDecimal riskCoefficient,
            int unusualnessIndex,
            boolean nightTransaction
    ) {
        return switch (rule.getRuleCode()) {
            case "HIGH_AMOUNT", "VERY_HIGH_AMOUNT" -> "amount=" + request.amount() + ", threshold=" + rule.getThresholdValue();
            case "NEW_DEVICE" -> "previousDevice=" + profile.getLastDeviceId() + ", currentDevice=" + request.deviceId();
            case "NEW_COUNTRY" -> "previousCountry=" + profile.getLastCountry() + ", currentCountry=" + request.country();
            case "NIGHT_TRANSACTION" -> "time=" + request.timestamp().toLocalTime() + ", nightWindow=" + nightTransaction;
            case "VELOCITY_24H" -> "txLastWindow=" + velocityCount(rule, historyStats) + ", threshold=" + rule.getThresholdValue()
                    + ", timeWindowSec=" + rule.getTimeWindowSec();
            case "HIGH_Z_SCORE" -> "zScore=" + zScore + ", avgAmount=" + historyStats.meanAmount()
                    + ", stdDev=" + historyStats.standardDeviationAmount();
            case "HIGH_RISK_COEFFICIENT" -> "riskCoefficient=" + riskCoefficient + ", threshold=" + rule.getThresholdValue();
            case "HIGH_UNUSUALNESS" -> "unusualnessIndex=" + unusualnessIndex + ", threshold=" + rule.getThresholdValue();
            default -> null;
        };
    }

    private boolean isVelocityTriggered(RuleConfigEntity rule, CustomerHistoryStats historyStats) {
        if (rule.getThresholdValue() == null) {
            return false;
        }
        return BigDecimal.valueOf(velocityCount(rule, historyStats)).compareTo(rule.getThresholdValue()) >= 0;
    }

    private long velocityCount(RuleConfigEntity rule, CustomerHistoryStats historyStats) {
        if (rule.getTimeWindowSec() == null || rule.getTimeWindowSec() <= 3600) {
            return historyStats.transactionsLastHour();
        }
        return historyStats.transactionsLast24Hours();
    }

    private BigDecimal calculateZScore(TransactionCheckRequest request, CustomerHistoryStats historyStats) {
        if (historyStats.historicalTransactionCount() < 3 || historyStats.standardDeviationAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal distance = request.amount().subtract(historyStats.meanAmount()).abs();
        return distance.divide(historyStats.standardDeviationAmount(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRiskCoefficient(
            TransactionCheckRequest request,
            CustomerHistoryStats historyStats,
            BigDecimal zScore,
            boolean newDevice,
            boolean newCountry,
            boolean nightTransaction
    ) {
        BigDecimal avgAmount = historyStats.meanAmount().compareTo(BigDecimal.ZERO) > 0
                ? historyStats.meanAmount()
                : request.amount();
        BigDecimal amountFactor = clamp01(request.amount().divide(avgAmount.max(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
        BigDecimal velocityFactor = clamp01(BigDecimal.valueOf(historyStats.transactionsLastHour())
                .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP));
        BigDecimal zFactor = clamp01(zScore.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
        BigDecimal deviceFactor = newDevice ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal countryFactor = newCountry ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal nightFactor = nightTransaction ? BigDecimal.valueOf(0.7) : BigDecimal.ZERO;

        BigDecimal weighted = zFactor.multiply(BigDecimal.valueOf(0.30))
                .add(velocityFactor.multiply(BigDecimal.valueOf(0.25)))
                .add(deviceFactor.multiply(BigDecimal.valueOf(0.15)))
                .add(countryFactor.multiply(BigDecimal.valueOf(0.12)))
                .add(nightFactor.multiply(BigDecimal.valueOf(0.08)))
                .add(amountFactor.multiply(BigDecimal.valueOf(0.10)));

        return clamp01(weighted).setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateUnusualnessIndex(
            TransactionCheckRequest request,
            CustomerHistoryStats historyStats,
            BigDecimal zScore,
            boolean newDevice,
            boolean newCountry,
            boolean nightTransaction
    ) {
        int index = 0;
        index += Math.min(zScore.multiply(BigDecimal.valueOf(12)).intValue(), 40);
        index += Math.min(historyStats.transactionsLastHour() * 8L, 24L);
        if (newDevice) {
            index += 18;
        }
        if (newCountry) {
            index += 12;
        }
        if (nightTransaction) {
            index += 10;
        }
        if (historyStats.meanAmount().compareTo(BigDecimal.ZERO) > 0
                && request.amount().compareTo(historyStats.meanAmount().multiply(BigDecimal.valueOf(2))) > 0) {
            index += 10;
        }
        return Math.min(index, 100);
    }

    private BigDecimal clamp01(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private boolean isNightTransaction(TransactionCheckRequest request) {
        LocalTime time = request.timestamp().toLocalTime();
        return time.isBefore(LocalTime.of(6, 0)) || !time.isBefore(LocalTime.of(23, 0));
    }

    private boolean isNewDevice(TransactionCheckRequest request, CustomerProfileEntity profile) {
        return profile != null
                && profile.getLastDeviceId() != null
                && request.deviceId() != null
                && !profile.getLastDeviceId().equals(request.deviceId());
    }

    private boolean isNewCountry(TransactionCheckRequest request, CustomerProfileEntity profile) {
        return profile != null
                && profile.getLastCountry() != null
                && request.country() != null
                && !profile.getLastCountry().equalsIgnoreCase(request.country());
    }

    private int compareAmount(BigDecimal amount, BigDecimal threshold) {
        if (amount == null || threshold == null) {
            return -1;
        }
        return amount.compareTo(threshold);
    }

    private Decision toDecision(int riskScore) {
        if (riskScore >= 70) {
            return Decision.DECLINE;
        }
        if (riskScore >= 40) {
            return Decision.REVIEW;
        }
        return Decision.ALLOW;
    }
}
