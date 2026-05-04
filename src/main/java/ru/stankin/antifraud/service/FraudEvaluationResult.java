package ru.stankin.antifraud.service;

import java.util.List;
import ru.stankin.antifraud.domain.Decision;

public record FraudEvaluationResult(
        int riskScore,
        Decision decision,
        List<TriggeredRule> triggeredRules,
        ScoringBreakdown scoringBreakdown
) {
}
