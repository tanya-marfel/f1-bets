package com.sporty.f1bets.shared.error;

import java.util.UUID;
import lombok.Getter;

@Getter
public class QuoteExpiredException extends RuntimeException {

    private final UUID quoteId;

    public QuoteExpiredException(UUID quoteId) {
        super("Quote expired: " + quoteId);
        this.quoteId = quoteId;
    }
}
