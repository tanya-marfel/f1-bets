package com.sporty.f1bets.betting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sporty.f1bets.betting.application.BetRepository;
import com.sporty.f1bets.betting.domain.BetStatus;
import com.sporty.f1bets.support.AbstractIntegrationTest;
import com.sporty.f1bets.support.FakeEventProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private BetRepository bets;

    @Test
    void onlyOneBetSucceedsWhenBalanceCoversOne() {
        long userId = newUserWith("100.00");
        // Two different events => different advisory locks => the user's optimistic
        // lock is what must protect the balance.
        String quoteA = quoteFor(200L, FakeEventProvider.WINNING_DRIVER);
        String quoteB = quoteFor(300L, FakeEventProvider.WINNING_DRIVER);

        List<ResponseEntity<Map<String, Object>>> results =
                runConcurrently(() -> placeBet(userId, quoteA, "75.00"), () -> placeBet(userId, quoteB, "75.00"));

        assertExactlyOneCreatedOneConflict(results);
    }

    @Test
    void sameQuoteCanBeUsedOnlyOnce() {
        long userId = newUserWith("100.00");
        String quote = quoteFor(400L, FakeEventProvider.WINNING_DRIVER);

        List<ResponseEntity<Map<String, Object>>> results =
                runConcurrently(() -> placeBet(userId, quote, "25.00"), () -> placeBet(userId, quote, "25.00"));

        assertExactlyOneCreatedOneConflict(results);
    }

    @Test
    void noPendingBetSurvivesConcurrentSettlement() {
        long userId = newUserWith("100.00");
        String quote = quoteFor(500L, FakeEventProvider.WINNING_DRIVER);

        runConcurrently(() -> placeBet(userId, quote, "25.00"), () -> settle(500L, FakeEventProvider.WINNING_DRIVER));

        // Invariant: the bet was either placed-then-settled or rejected outright;
        // it can never linger as PENDING on a settled event.
        assertThat(bets.countByEventIdAndStatus(500L, BetStatus.PENDING)).isZero();
    }

    private void assertExactlyOneCreatedOneConflict(List<ResponseEntity<Map<String, Object>>> results) {
        long created = results.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CREATED)
                .count();
        long conflict = results.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CONFLICT)
                .count();
        assertThat(created).isEqualTo(1);
        assertThat(conflict).isEqualTo(1);
    }

    @SafeVarargs
    private List<ResponseEntity<Map<String, Object>>> runConcurrently(
            Supplier<ResponseEntity<Map<String, Object>>>... actions) {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(actions.length);
        List<CompletableFuture<ResponseEntity<Map<String, Object>>>> futures = new ArrayList<>();
        for (Supplier<ResponseEntity<Map<String, Object>>> action : actions) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> {
                        await(start);
                        return action.get();
                    },
                    executor));
        }
        start.countDown();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        executor.shutdown();
        List<ResponseEntity<Map<String, Object>>> results = new ArrayList<>();
        for (CompletableFuture<ResponseEntity<Map<String, Object>>> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
