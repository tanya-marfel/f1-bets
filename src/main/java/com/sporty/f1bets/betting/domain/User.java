package com.sporty.f1bets.betting.domain;

import com.sporty.f1bets.shared.error.InsufficientFundsException;
import com.sporty.f1bets.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A registered user with a EUR balance. The {@code @Version} column drives
 * optimistic locking so concurrent balance changes cannot silently corrupt it.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance_eur", nullable = false)
    private Money balance;

    @Version
    private long version;

    public User(Money balance) {
        this.balance = balance;
    }

    public void debit(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(id == null ? -1 : id);
        }
        this.balance = balance.subtract(amount);
    }

    public void credit(Money amount) {
        this.balance = balance.add(amount);
    }
}
