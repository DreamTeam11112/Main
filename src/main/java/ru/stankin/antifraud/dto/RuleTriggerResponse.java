package ru.stankin.antifraud.dto;

public record RuleTriggerResponse(
        String ruleCode,
        Integer scoreAdded
) {
}
