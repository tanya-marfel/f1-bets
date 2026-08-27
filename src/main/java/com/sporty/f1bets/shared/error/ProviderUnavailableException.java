package com.sporty.f1bets.shared.error;

public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
