package com.sporty.f1bets.shared.error;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final long userId;

    public UserNotFoundException(long userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
}
