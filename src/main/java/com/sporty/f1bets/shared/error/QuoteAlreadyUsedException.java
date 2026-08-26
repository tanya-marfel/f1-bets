package com.sporty.f1bets.shared.error;

import java.util.UUID;

public class QuoteAlreadyUsedException extends RuntimeException {

    private final UUID quoteId;

    public QuoteAlreadyUsedException(UUID quoteId) {
        super("Quote already used: " + quoteId);
        this.quoteId = quoteId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }
}

