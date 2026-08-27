package com.sporty.f1bets.betting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sporty.f1bets.support.AbstractIntegrationTest;
import com.sporty.f1bets.support.FakeEventProvider;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BettingE2ETest extends AbstractIntegrationTest {

    private static BigDecimal decimal(Map<String, Object> body, String field) {
        return new BigDecimal(String.valueOf(body.get(field)));
    }

    private static int integer(Map<String, Object> body, String field) {
        return ((Number) body.get(field)).intValue();
    }

    @Test
    void listPlaceSettleAndGuards() {
        long userId = newUserWith("100.00");
        String quote = quoteFor(100L, FakeEventProvider.WINNING_DRIVER);

        ResponseEntity<Map<String, Object>> bet = placeBet(userId, quote, "25.00");
        assertThat(bet.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bet.getBody()).containsEntry("status", "PENDING");
        assertThat(decimal(bet.getBody(), "newBalanceEur")).isEqualByComparingTo("75.00");

        ResponseEntity<Map<String, Object>> outcome = settle(100L, FakeEventProvider.WINNING_DRIVER);
        assertThat(outcome.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(integer(outcome.getBody(), "wonBets")).isEqualTo(1);
        assertThat(integer(outcome.getBody(), "lostBets")).isZero();
        assertThat(decimal(outcome.getBody(), "totalPaidOutEur")).isEqualByComparingTo("100.00");

        // Duplicate settlement is rejected (idempotency guard).
        assertThat(settle(100L, FakeEventProvider.WINNING_DRIVER).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // A bet on an already-settled event is rejected, even with a fresh valid quote.
        String lateQuote = quoteFor(100L, FakeEventProvider.LOSING_DRIVER);
        assertThat(placeBet(userId, lateQuote, "10.00").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void insufficientFundsIsRejected() {
        long userId = newUserWith("10.00");
        String quote = quoteFor(200L, FakeEventProvider.WINNING_DRIVER);

        ResponseEntity<Map<String, Object>> bet = placeBet(userId, quote, "25.00");

        assertThat(bet.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bet.getBody()).containsEntry("title", "Insufficient funds");
    }

    @Test
    void validationErrorIsBadRequest() {
        ResponseEntity<Map<String, Object>> response = placeRawBet(Map.of("userId", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void quoteNotFoundReturnsRfc7807Shape() {
        long userId = newUserWith("100.00");
        String unknownQuote = UUID.randomUUID().toString();

        ResponseEntity<Map<String, Object>> response = placeBet(userId, unknownQuote, "10.00");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> body = response.getBody();
        assertThat(String.valueOf(body.get("type"))).contains("quote-not-found");
        assertThat(body).containsEntry("title", "Quote not found");
        assertThat(integer(body, "status")).isEqualTo(404);
        assertThat(String.valueOf(body.get("detail"))).isNotBlank();
        assertThat(body).containsEntry("quoteId", unknownQuote);
    }
}
