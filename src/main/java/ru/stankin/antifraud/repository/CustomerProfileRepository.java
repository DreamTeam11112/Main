package ru.stankin.antifraud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.stankin.antifraud.entity.CustomerProfileEntity;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long> {

    Optional<CustomerProfileEntity> findByCustomerId(String customerId);
}
