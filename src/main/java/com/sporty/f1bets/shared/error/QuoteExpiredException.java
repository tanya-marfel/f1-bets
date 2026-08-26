package com.sporty.f1bets.shared.error;

import java.util.UUID;

public class QuoteExpiredException extends RuntimeException {

    private final UUID quoteId;

    public QuoteExpiredException(UUID quoteId) {
        super("Quote expired: " + quoteId);
        this.quoteId = quoteId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }
}

