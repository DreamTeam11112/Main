package ru.stankin.antifraud.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stankin.antifraud.dto.PagedResponse;
import ru.stankin.antifraud.dto.RuleTriggerResponse;
import ru.stankin.antifraud.dto.TransactionCheckRequest;
import ru.stankin.antifraud.dto.TransactionDecisionResponse;
import ru.stankin.antifraud.dto.TransactionListItemResponse;
import ru.stankin.antifraud.entity.CustomerProfileEntity;
import ru.stankin.antifraud.entity.FraudDecisionEntity;
import ru.stankin.antifraud.entity.RuleConfigEntity;
import ru.stankin.antifraud.entity.RuleTriggerEntity;
import ru.stankin.antifraud.entity.TransactionEntity;
import ru.stankin.antifraud.exception.DuplicateTransactionException;
import ru.stankin.antifraud.exception.ResourceNotFoundException;
import ru.stankin.antifraud.repository.CustomerProfileRepository;
import ru.stankin.antifraud.repository.FraudDecisionRepository;
import ru.stankin.antifraud.repository.RuleConfigRepository;
import ru.stankin.antifraud.repository.RuleTriggerRepository;
import ru.stankin.antifraud.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudDecisionRepository fraudDecisionRepository;
    private final RuleTriggerRepository ruleTriggerRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final FraudRuleEngine fraudRuleEngine;

    public TransactionService(
            TransactionRepository transactionRepository,
            FraudDecisionRepository fraudDecisionRepository,
            RuleTriggerRepository ruleTriggerRepository,
            RuleConfigRepository ruleConfigRepository,
            CustomerProfileRepository customerProfileRepository,
            FraudRuleEngine fraudRuleEngine
    ) {
        this.transactionRepository = transactionRepository;
        this.fraudDecisionRepository = fraudDecisionRepository;
        this.ruleTriggerRepository = ruleTriggerRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @Transactional
    public TransactionDecisionResponse checkTransaction(TransactionCheckRequest request) {
        if (transactionRepository.existsByTransactionId(request.transactionId())) {
            throw new DuplicateTransactionException(request.transactionId());
        }

        TransactionEntity transaction = toEntity(request);
        transaction = transactionRepository.save(transaction);

        CustomerProfileEntity profile = customerProfileRepository.findByCustomerId(request.customerId()).orElse(null);
        List<TransactionEntity> history = transactionRepository
                .findTop200ByCustomerIdAndTransactionIdNotOrderByEventTimeDesc(request.customerId(), request.transactionId());
        CustomerHistoryStats historyStats = buildHistoryStats(request, history);
        List<RuleConfigEntity> rules = ruleConfigRepository.findAllByOrderByRuleCodeAsc();

        FraudEvaluationResult evaluation = fraudRuleEngine.evaluate(request, profile, historyStats, rules);

        FraudDecisionEntity decision = new FraudDecisionEntity();
        decision.setTransaction(transaction);
        decision.setRiskScore(evaluation.riskScore());
        decision.setDecision(evaluation.decision());
        decision.setDecisionReason(String.join(", ",
                evaluation.triggeredRules().stream().map(TriggeredRule::ruleCode).toList()));
        decision.setProcessedAt(LocalDateTime.now());
        fraudDecisionRepository.save(decision);

        for (TriggeredRule triggeredRule : evaluation.triggeredRules()) {
            RuleTriggerEntity trigger = new RuleTriggerEntity();
            trigger.setTransaction(transaction);
            trigger.setRuleCode(triggeredRule.ruleCode());
            trigger.setRuleName(triggeredRule.ruleName());
            trigger.setScoreAdded(triggeredRule.scoreAdded());
            trigger.setDetails(triggeredRule.details());
            trigger.setCreatedAt(LocalDateTime.now());
            ruleTriggerRepository.save(trigger);
        }

        updateCustomerProfile(profile, request, historyStats);

        return getTransactionDecision(request.transactionId());
    }

    @Transactional(readOnly = true)
    public TransactionDecisionResponse getTransactionDecision(String transactionId) {
        FraudDecisionEntity decision = fraudDecisionRepository.findByTransaction_TransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("transaction not found: " + transactionId));

        List<RuleTriggerResponse> triggers = ruleTriggerRepository.findAllByTransaction_TransactionIdOrderByIdAsc(transactionId)
                .stream()
                .map(trigger -> new RuleTriggerResponse(trigger.getRuleCode(), trigger.getScoreAdded()))
                .toList();

        return new TransactionDecisionResponse(
                decision.getTransaction().getTransactionId(),
                decision.getRiskScore(),
                decision.getDecision().name(),
                decision.getProcessedAt(),
                triggers
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionListItemResponse> getTransactions(int page, int size) {
        Page<TransactionEntity> transactions = transactionRepository.findAllByOrderByEventTimeDesc(PageRequest.of(page, size));
        List<TransactionListItemResponse> items = transactions.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new PagedResponse<>(transactions.getNumber(), transactions.getSize(), transactions.getTotalElements(), items);
    }

    private TransactionListItemResponse toListItem(TransactionEntity transaction) {
        FraudDecisionEntity decision = fraudDecisionRepository.findByTransaction_TransactionId(transaction.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("decision not found for: " + transaction.getTransactionId()));

        return new TransactionListItemResponse(
                transaction.getTransactionId(),
                transaction.getCustomerId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getEventTime(),
                decision.getDecision().name(),
                decision.getRiskScore()
        );
    }

    private TransactionEntity toEntity(TransactionCheckRequest request) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(request.transactionId());
        entity.setCustomerId(request.customerId());
        entity.setMerchantId(request.merchantId());
        entity.setCardToken(request.cardToken());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency().toUpperCase());
        entity.setEventTime(request.timestamp());
        entity.setIpAddress(request.ipAddress());
        entity.setDeviceId(request.deviceId());
        entity.setCountry(request.country());
        entity.setCity(request.city());
        entity.setPaymentMethod(request.paymentMethod());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private CustomerHistoryStats buildHistoryStats(TransactionCheckRequest request, List<TransactionEntity> history) {
        List<BigDecimal> amounts = history.stream()
                .map(TransactionEntity::getAmount)
                .toList();
        BigDecimal meanAmount = calculateMean(amounts);
        BigDecimal stdDev = calculateStandardDeviation(amounts, meanAmount);
        long transactionsLastHour = history.stream()
                .filter(transaction -> !transaction.getEventTime().isBefore(request.timestamp().minusHours(1)))
                .count();
        long transactionsLast24Hours = history.stream()
                .filter(transaction -> !transaction.getEventTime().isBefore(request.timestamp().minusHours(24)))
                .count();

        return new CustomerHistoryStats(
                history,
                transactionRepository.countByCustomerId(request.customerId()) - 1,
                meanAmount,
                stdDev,
                transactionsLastHour,
                transactionsLast24Hours
        );
    }

    private BigDecimal calculateMean(List<BigDecimal> amounts) {
        if (amounts.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> amounts, BigDecimal meanAmount) {
        if (amounts.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double mean = meanAmount.doubleValue();
        double variance = amounts.stream()
                .mapToDouble(amount -> Math.pow(amount.doubleValue() - mean, 2))
                .average()
                .orElse(0.0d);

        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateCustomerProfile(CustomerProfileEntity profile, TransactionCheckRequest request, CustomerHistoryStats historyStats) {
        CustomerProfileEntity currentProfile = profile;
        if (currentProfile == null) {
            currentProfile = new CustomerProfileEntity();
            currentProfile.setCustomerId(request.customerId());
        } else {
            currentProfile.setCustomerId(request.customerId());
        }

        BigDecimal totalAmount = historyStats.meanAmount().multiply(BigDecimal.valueOf(historyStats.history().size()))
                .add(request.amount());
        BigDecimal newAverage = historyStats.history().isEmpty()
                ? request.amount().setScale(2, RoundingMode.HALF_UP)
                : totalAmount.divide(BigDecimal.valueOf(historyStats.history().size() + 1L), 2, RoundingMode.HALF_UP);

        currentProfile.setAvgAmount(newAverage);
        currentProfile.setTxCount24h((int) (historyStats.transactionsLast24Hours() + 1));
        currentProfile.setLastCountry(request.country());
        currentProfile.setLastCity(request.city());
        currentProfile.setLastDeviceId(request.deviceId());
        currentProfile.setLastTransactionTime(request.timestamp());
        currentProfile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(currentProfile);
    }
}
