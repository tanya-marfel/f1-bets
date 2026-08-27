package com.sporty.f1bets.betting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sporty.f1bets.shared.error.InsufficientFundsException;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;

@Small
class UserTest {

    @Test
    void debitReducesBalance() {
        User user = new User(Money.of("100.00"));
        user.debit(Money.of("30.00"));
        assertThat(user.getBalance()).isEqualTo(Money.of("70.00"));
    }

    @Test
    void debitEqualToBalanceIsAllowed() {
        User user = new User(Money.of("100.00"));
        user.debit(Money.of("100.00"));
        assertThat(user.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void debitBeyondBalanceThrows() {
        User user = new User(Money.of("100.00"));
        Money amount = Money.of("100.01");

        assertThatThrownBy(() -> user.debit(amount)).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void creditIncreasesBalance() {
        User user = new User(Money.of("100.00"));
        user.credit(Money.of("50.00"));
        assertThat(user.getBalance()).isEqualTo(Money.of("150.00"));
    }
}
