package ru.stankin.antifraud.service;

public record TriggeredRule(
        String ruleCode,
        String ruleName,
        int scoreAdded,
        String details
) {
}
