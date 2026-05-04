package ru.stankin.antifraud.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.stankin.antifraud.dto.PagedResponse;
import ru.stankin.antifraud.dto.TransactionCheckRequest;
import ru.stankin.antifraud.dto.TransactionDecisionResponse;
import ru.stankin.antifraud.dto.TransactionListItemResponse;
import ru.stankin.antifraud.service.TransactionService;

@Validated
@RestController
@RequestMapping({"/api/v1/transactions", "/transactions"})
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/check")
    public TransactionDecisionResponse checkTransaction(@Valid @RequestBody TransactionCheckRequest request) {
        return transactionService.checkTransaction(request);
    }

    @GetMapping("/{transactionId}")
    public TransactionDecisionResponse getTransaction(@PathVariable String transactionId) {
        return transactionService.getTransactionDecision(transactionId);
    }

    @GetMapping
    public PagedResponse<TransactionListItemResponse> getTransactions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return transactionService.getTransactions(page, size);
    }
}
