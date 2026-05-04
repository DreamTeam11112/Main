package ru.stankin.antifraud.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.stankin.antifraud.entity.RuleConfigEntity;

public interface RuleConfigRepository extends JpaRepository<RuleConfigEntity, Long> {

    Optional<RuleConfigEntity> findByRuleCode(String ruleCode);

    List<RuleConfigEntity> findAllByOrderByRuleCodeAsc();

    List<RuleConfigEntity> findAllByEnabledTrue();
}
