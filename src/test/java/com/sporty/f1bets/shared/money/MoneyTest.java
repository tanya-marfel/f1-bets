package com.sporty.f1bets.shared.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.sporty.f1bets.testing.Small;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Small
class MoneyTest {

    @Test
    void normalizesToScaleTwo() {
        assertThat(Money.of("100").toBigDecimal()).isEqualByComparingTo("100.00");
        assertThat(Money.of(new BigDecimal("2.5")).toBigDecimal().scale()).isEqualTo(2);
    }

    @Test
    void addsAndSubtracts() {
        assertThat(Money.of("100.00").subtract(Money.of("30.00"))).isEqualTo(Money.of("70.00"));
        assertThat(Money.of("70.00").add(Money.of("30.00"))).isEqualTo(Money.of("100.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "25.00, 2, 50.00",
            "25.00, 3, 75.00",
            "25.00, 4, 100.00"
    })
    void multipliesByOdds(String stake, int odds, String expected) {
        assertThat(Money.of(stake).multiply(odds)).isEqualTo(Money.of(expected));
    }

    @Test
    void comparisons() {
        assertThat(Money.of("10.00").isLessThan(Money.of("10.01"))).isTrue();
        assertThat(Money.of("10.00").isPositive()).isTrue();
        assertThat(Money.zero().isPositive()).isFalse();
        assertThat(Money.of("-1.00").isNegative()).isTrue();
    }

    @Test
    void equalityIsByValue() {
        assertThat(Money.of("100.0")).isEqualTo(Money.of("100.00"));
        assertThat(Money.of("100.0")).hasSameHashCodeAs(Money.of("100.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "2.005, 2.00",
            "2.015, 2.02",
            "2.025, 2.02",
            "2.035, 2.04"
    })
    void roundsHalfEven(String input, String expected) {
        assertThat(Money.of(input).toBigDecimal()).isEqualByComparingTo(expected);
    }
}


