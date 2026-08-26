package com.sporty.f1bets.shared.error;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {

    private final UUID quoteId;

    public QuoteNotFoundException(UUID quoteId) {
        super("Quote not found: " + quoteId);
        this.quoteId = quoteId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }
}

