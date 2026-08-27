package com.sporty.f1bets.shared.odds;

import static org.assertj.core.api.Assertions.assertThat;

import com.sporty.f1bets.testing.Small;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Small
class RandomOddsGeneratorTest {

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L, 100L, 2026L})
    void alwaysReturnsAllowedValues(long seed) {
        OddsGenerator generator = new RandomOddsGenerator(new Random(seed));
        assertThat(IntStream.range(0, 1000).map(i -> generator.nextOdds()))
                .allMatch(odds -> odds == 2 || odds == 3 || odds == 4);
    }

    @Test
    void isDeterministicForAGivenSeed() {
        OddsGenerator a = new RandomOddsGenerator(new Random(42));
        OddsGenerator b = new RandomOddsGenerator(new Random(42));
        assertThat(a.nextOdds()).isEqualTo(b.nextOdds());
        assertThat(a.nextOdds()).isEqualTo(b.nextOdds());
    }

    @Test
    void producesEachAllowedValueAcrossManyRolls() {
        OddsGenerator generator = new RandomOddsGenerator(new Random(7));
        assertThat(IntStream.range(0, 1000)
                        .map(i -> generator.nextOdds())
                        .distinct()
                        .sorted()
                        .toArray())
                .containsExactly(2, 3, 4);
    }
}
