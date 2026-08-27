package com.sporty.f1bets.shared.quote;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OddsQuoteRepository extends JpaRepository<OddsQuote, UUID> {}
