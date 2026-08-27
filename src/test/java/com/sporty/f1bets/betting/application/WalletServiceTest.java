package com.sporty.f1bets.betting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sporty.f1bets.betting.domain.User;
import com.sporty.f1bets.shared.error.InsufficientFundsException;
import com.sporty.f1bets.shared.error.UserNotFoundException;
import com.sporty.f1bets.shared.money.Money;
import com.sporty.f1bets.testing.Small;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@Small
class WalletServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final WalletService walletService = new WalletService(users);

    @Test
    void debitsBalance() {
        when(users.findById(1L)).thenReturn(Optional.of(new User(Money.of("100.00"))));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = walletService.debit(1L, Money.of("40.00"));

        assertThat(result.getBalance()).isEqualTo(Money.of("60.00"));
    }

    @Test
    void creditsBalance() {
        when(users.findById(1L)).thenReturn(Optional.of(new User(Money.of("100.00"))));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = walletService.credit(1L, Money.of("25.00"));

        assertThat(result.getBalance()).isEqualTo(Money.of("125.00"));
    }

    @Test
    void debitBeyondBalanceThrows() {
        when(users.findById(1L)).thenReturn(Optional.of(new User(Money.of("10.00"))));
        Money amount = Money.of("10.01");

        assertThatThrownBy(() -> walletService.debit(1L, amount)).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void unknownUserThrows() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        Money amount = Money.of("1.00");

        assertThatThrownBy(() -> walletService.credit(99L, amount)).isInstanceOf(UserNotFoundException.class);
    }
}
