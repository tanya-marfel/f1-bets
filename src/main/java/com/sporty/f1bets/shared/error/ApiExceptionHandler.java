package com.sporty.f1bets.shared.error;

import java.net.URI;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into RFC 7807 problem+json responses with a
 * consistent shape (type, title, status, detail) plus relevant extension
 * fields (quoteId, eventId, userId).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE_TYPE = "https://f1-bets/errors/";

    @ExceptionHandler(QuoteNotFoundException.class)
    public ProblemDetail handle(QuoteNotFoundException ex) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "Quote not found", "quote-not-found", ex.getMessage());
        problem.setProperty("quoteId", ex.getQuoteId());
        return problem;
    }

    @ExceptionHandler(QuoteExpiredException.class)
    public ProblemDetail handle(QuoteExpiredException ex) {
        ProblemDetail problem = problem(HttpStatus.GONE, "Quote expired", "quote-expired", ex.getMessage());
        problem.setProperty("quoteId", ex.getQuoteId());
        return problem;
    }

    @ExceptionHandler(QuoteAlreadyUsedException.class)
    public ProblemDetail handle(QuoteAlreadyUsedException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Quote already used", "quote-already-used", ex.getMessage());
        problem.setProperty("quoteId", ex.getQuoteId());
        return problem;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handle(InsufficientFundsException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Insufficient funds", "insufficient-funds", ex.getMessage());
        problem.setProperty("userId", ex.getUserId());
        return problem;
    }

    @ExceptionHandler(EventAlreadySettledException.class)
    public ProblemDetail handle(EventAlreadySettledException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Event already settled", "event-already-settled", ex.getMessage());
        problem.setProperty("eventId", ex.getEventId());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handle(UserNotFoundException ex) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "User not found", "user-not-found", ex.getMessage());
        problem.setProperty("userId", ex.getUserId());
        return problem;
    }

    @ExceptionHandler(InvalidBetAmountException.class)
    public ProblemDetail handle(InvalidBetAmountException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid bet amount", "invalid-bet-amount", ex.getMessage());
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ProblemDetail handle(ProviderUnavailableException ex) {
        return problem(HttpStatus.BAD_GATEWAY, "Event provider unavailable", "provider-unavailable", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handle(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification", "concurrent-modification",
                "The resource was modified concurrently, please retry.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + type));
        return problem;
    }
}

