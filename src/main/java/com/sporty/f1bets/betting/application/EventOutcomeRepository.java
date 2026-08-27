package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.EventOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutcomeRepository extends JpaRepository<EventOutcome, Long> {}
