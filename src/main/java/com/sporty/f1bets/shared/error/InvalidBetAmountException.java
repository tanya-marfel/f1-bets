package com.sporty.f1bets.shared.error;

public class InvalidBetAmountException extends RuntimeException {

    public InvalidBetAmountException(String message) {
        super(message);
    }
}
