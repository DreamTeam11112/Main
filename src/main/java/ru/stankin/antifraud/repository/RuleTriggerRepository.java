package ru.stankin.antifraud.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.stankin.antifraud.entity.RuleTriggerEntity;

public interface RuleTriggerRepository extends JpaRepository<RuleTriggerEntity, Long> {

    List<RuleTriggerEntity> findAllByTransaction_TransactionIdOrderByIdAsc(String transactionId);
}
