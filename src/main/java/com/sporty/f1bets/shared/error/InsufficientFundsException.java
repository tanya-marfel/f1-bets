package com.sporty.f1bets.shared.error;

public class InsufficientFundsException extends RuntimeException {

    private final long userId;

    public InsufficientFundsException(long userId) {
        super("Insufficient funds for user " + userId);
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }
}

