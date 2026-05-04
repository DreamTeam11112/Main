package ru.stankin.antifraud.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.stankin.antifraud.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    boolean existsByTransactionId(String transactionId);

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    long countByCustomerId(String customerId);

    List<TransactionEntity> findTop200ByCustomerIdAndTransactionIdNotOrderByEventTimeDesc(String customerId, String transactionId);

    @Query("""
            select count(t)
            from TransactionEntity t
            where t.customerId = :customerId
              and t.eventTime >= :fromTime
            """)
    long countCustomerTransactionsSince(String customerId, LocalDateTime fromTime);

    Page<TransactionEntity> findAllByOrderByEventTimeDesc(Pageable pageable);
}
