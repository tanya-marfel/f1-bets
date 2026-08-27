package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.Bet;
import com.sporty.f1bets.betting.domain.BetStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByEventIdAndStatus(long eventId, BetStatus status);

    long countByEventIdAndStatus(long eventId, BetStatus status);
}
