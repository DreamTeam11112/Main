package ru.stankin.antifraud.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stankin.antifraud.dto.RuleResponse;
import ru.stankin.antifraud.dto.RuleUpdateRequest;
import ru.stankin.antifraud.service.RuleService;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<RuleResponse> getRules() {
        return ruleService.getRules();
    }

    @GetMapping("/{ruleCode}")
    public RuleResponse getRule(@PathVariable String ruleCode) {
        return ruleService.getRule(ruleCode);
    }

    @PutMapping("/{ruleCode}")
    public RuleResponse updateRule(@PathVariable String ruleCode, @Valid @RequestBody RuleUpdateRequest request) {
        return ruleService.updateRule(ruleCode, request);
    }
}
