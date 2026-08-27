package com.sporty.f1bets.shared.error;

import lombok.Getter;

@Getter
public class InsufficientFundsException extends RuntimeException {

    private final long userId;

    public InsufficientFundsException(long userId) {
        super("Insufficient funds for user " + userId);
        this.userId = userId;
    }
}
