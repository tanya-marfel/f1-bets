package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.error.UserNotFoundException;
import com.sporty.f1bets.shared.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Debits and credits user balances. Kept as a single collaborator so balance
 * mutation (and its optimistic-lock semantics) lives in one place.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository users;


    public User debit(long userId, Money amount) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.debit(amount);
        return users.save(user);
    }

    public User credit(long userId, Money amount) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.credit(amount);
        return users.save(user);
    }
}

