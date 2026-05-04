package ru.stankin.antifraud.service;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stankin.antifraud.dto.RuleResponse;
import ru.stankin.antifraud.dto.RuleUpdateRequest;
import ru.stankin.antifraud.entity.RuleConfigEntity;
import ru.stankin.antifraud.exception.BadRequestException;
import ru.stankin.antifraud.exception.ResourceNotFoundException;
import ru.stankin.antifraud.repository.RuleConfigRepository;

@Service
public class RuleService {

    private final RuleConfigRepository ruleConfigRepository;

    public RuleService(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> getRules() {
        return ruleConfigRepository.findAllByOrderByRuleCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RuleResponse getRule(String ruleCode) {
        return ruleConfigRepository.findByRuleCode(ruleCode.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("rule not found: " + ruleCode));
    }

    @Transactional
    public RuleResponse updateRule(String ruleCode, RuleUpdateRequest request) {
        if (!ruleCode.equalsIgnoreCase(request.ruleCode())) {
            throw new BadRequestException("path ruleCode must match request body ruleCode");
        }

        return ruleConfigRepository.findByRuleCode(ruleCode.toUpperCase())
                .map(rule -> {
                    rule.setWeight(request.weight());
                    rule.setThresholdValue(request.threshold());
                    rule.setEnabled(request.enabled());
                    rule.setUpdatedAt(LocalDateTime.now());
                    return toResponse(ruleConfigRepository.save(rule));
                })
                .orElseThrow(() -> new ResourceNotFoundException("rule not found: " + ruleCode));
    }

    private RuleResponse toResponse(RuleConfigEntity entity) {
        return new RuleResponse(
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.getWeight(),
                entity.getThresholdValue(),
                entity.getEnabled(),
                entity.getUpdatedAt()
        );
    }
}
