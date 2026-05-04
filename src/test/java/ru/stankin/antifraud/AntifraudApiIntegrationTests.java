package ru.stankin.antifraud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.stankin.antifraud.dto.RuleUpdateRequest;
import ru.stankin.antifraud.dto.TransactionCheckRequest;
import ru.stankin.antifraud.entity.CustomerProfileEntity;
import ru.stankin.antifraud.entity.FraudDecisionEntity;
import ru.stankin.antifraud.entity.TransactionEntity;
import ru.stankin.antifraud.repository.CustomerProfileRepository;
import ru.stankin.antifraud.repository.FraudDecisionRepository;
import ru.stankin.antifraud.repository.RuleTriggerRepository;
import ru.stankin.antifraud.repository.TransactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AntifraudApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudDecisionRepository fraudDecisionRepository;

    @Autowired
    private RuleTriggerRepository ruleTriggerRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @BeforeEach
    void setUp() {
        ruleTriggerRepository.deleteAll();
        fraudDecisionRepository.deleteAll();
        transactionRepository.deleteAll();
        customerProfileRepository.deleteAll();
    }

    @Test
    void shouldEvaluateTransactionAndReturnDeclineWithAdvancedTriggers() throws Exception {
        seedCustomerProfile("CUST-1", "DEVICE-OLD", "RU");
        seedHistoricalTransaction("HIST-1", "CUST-1", new BigDecimal("1000.00"), LocalDateTime.of(2026, 4, 17, 0, 35));
        seedHistoricalTransaction("HIST-2", "CUST-1", new BigDecimal("1100.00"), LocalDateTime.of(2026, 4, 17, 0, 45));
        seedHistoricalTransaction("HIST-3", "CUST-1", new BigDecimal("900.00"), LocalDateTime.of(2026, 4, 17, 0, 55));
        seedHistoricalTransaction("HIST-4", "CUST-1", new BigDecimal("950.00"), LocalDateTime.of(2026, 4, 17, 1, 5));
        seedHistoricalTransaction("HIST-5", "CUST-1", new BigDecimal("1050.00"), LocalDateTime.of(2026, 4, 17, 1, 15));

        TransactionCheckRequest request = new TransactionCheckRequest(
                "TRX-100001",
                "CUST-1",
                "SHOP-001",
                "CARD-001",
                new BigDecimal("70000.00"),
                "RUB",
                LocalDateTime.of(2026, 4, 17, 1, 30),
                "91.142.33.10",
                "DEVICE-NEW",
                "RU",
                "Moscow",
                "BANK_CARD"
        );

        mockMvc.perform(post("/api/v1/transactions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TRX-100001"))
                .andExpect(jsonPath("$.decision").value("DECLINE"))
                .andExpect(jsonPath("$.riskScore").value(130))
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='HIGH_AMOUNT')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='NEW_DEVICE')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='NIGHT_TRANSACTION')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='VELOCITY_24H')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='HIGH_Z_SCORE')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='HIGH_RISK_COEFFICIENT')]").exists())
                .andExpect(jsonPath("$.triggers[?(@.ruleCode=='HIGH_UNUSUALNESS')]").exists());

        mockMvc.perform(get("/api/v1/transactions/TRX-100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TRX-100001"))
                .andExpect(jsonPath("$.decision").value("DECLINE"));
    }

    @Test
    void shouldReturnConflictForDuplicateTransactionId() throws Exception {
        TransactionCheckRequest request = new TransactionCheckRequest(
                "TRX-DUPLICATE",
                "CUST-2",
                "SHOP-001",
                "CARD-001",
                new BigDecimal("1500.00"),
                "RUB",
                LocalDateTime.of(2026, 4, 17, 14, 0),
                "91.142.33.10",
                "DEVICE-01",
                "RU",
                "Moscow",
                "BANK_CARD"
        );

        mockMvc.perform(post("/api/v1/transactions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transactions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_TRANSACTION"))
                .andExpect(jsonPath("$.field").value("transactionId"));
    }

    @Test
    void shouldReturnPagedTransactionsAndAllowRuleUpdate() throws Exception {
        TransactionCheckRequest requestOne = new TransactionCheckRequest(
                "TRX-LIST-1",
                "CUST-3",
                "SHOP-001",
                "CARD-001",
                new BigDecimal("1200.00"),
                "RUB",
                LocalDateTime.of(2026, 4, 17, 10, 0),
                "91.142.33.10",
                "DEVICE-11",
                "RU",
                "Moscow",
                "BANK_CARD"
        );
        TransactionCheckRequest requestTwo = new TransactionCheckRequest(
                "TRX-LIST-2",
                "CUST-4",
                "SHOP-002",
                "CARD-002",
                new BigDecimal("88000.00"),
                "RUB",
                LocalDateTime.of(2026, 4, 17, 11, 0),
                "91.142.33.11",
                "DEVICE-12",
                "RU",
                "Moscow",
                "BANK_CARD"
        );

        mockMvc.perform(post("/api/v1/transactions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOne)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/transactions/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTwo)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ruleCode=='HIGH_AMOUNT')]").exists());

        mockMvc.perform(get("/api/v1/rules/HIGH_AMOUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCode").value("HIGH_AMOUNT"));

        RuleUpdateRequest updateRequest = new RuleUpdateRequest("HIGH_AMOUNT", 30, new BigDecimal("45000.00"), true);

        mockMvc.perform(put("/api/v1/rules/HIGH_AMOUNT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCode").value("HIGH_AMOUNT"))
                .andExpect(jsonPath("$.weight").value(30))
                .andExpect(jsonPath("$.threshold").value(45000.00));
    }

    private void seedCustomerProfile(String customerId, String deviceId, String country) {
        CustomerProfileEntity profile = new CustomerProfileEntity();
        profile.setCustomerId(customerId);
        profile.setAvgAmount(new BigDecimal("1000.00"));
        profile.setTxCount24h(3);
        profile.setLastCountry(country);
        profile.setLastCity("Moscow");
        profile.setLastDeviceId(deviceId);
        profile.setLastTransactionTime(LocalDateTime.of(2026, 4, 17, 0, 50));
        profile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(profile);
    }

    private void seedHistoricalTransaction(String transactionId, String customerId, BigDecimal amount, LocalDateTime time) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(transactionId);
        transaction.setCustomerId(customerId);
        transaction.setMerchantId("SHOP-HIST");
        transaction.setCardToken("CARD-HIST");
        transaction.setAmount(amount);
        transaction.setCurrency("RUB");
        transaction.setEventTime(time);
        transaction.setIpAddress("127.0.0.1");
        transaction.setDeviceId("DEVICE-OLD");
        transaction.setCountry("RU");
        transaction.setCity("Moscow");
        transaction.setPaymentMethod("BANK_CARD");
        transaction.setCreatedAt(LocalDateTime.now());
        TransactionEntity saved = transactionRepository.save(transaction);

        FraudDecisionEntity decision = new FraudDecisionEntity();
        decision.setTransaction(saved);
        decision.setRiskScore(5);
        decision.setDecision(ru.stankin.antifraud.domain.Decision.ALLOW);
        decision.setDecisionReason("history");
        decision.setProcessedAt(time.plusSeconds(1));
        fraudDecisionRepository.save(decision);
    }
}
