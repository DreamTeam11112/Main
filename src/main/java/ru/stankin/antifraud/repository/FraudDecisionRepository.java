package ru.stankin.antifraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.stankin.antifraud.entity.FraudDecisionEntity;

public interface FraudDecisionRepository extends JpaRepository<FraudDecisionEntity, Long> {

    @EntityGraph(attributePaths = "transaction")
    Optional<FraudDecisionEntity> findByTransaction_TransactionId(String transactionId);
}
