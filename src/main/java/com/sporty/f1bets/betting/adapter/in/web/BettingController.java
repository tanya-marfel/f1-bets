package com.sporty.f1bets.betting.adapter.in.web;

import com.sporty.f1bets.betting.application.PlaceBetService;
import com.sporty.f1bets.betting.application.PlacedBet;
import com.sporty.f1bets.generated.api.BetsApi;
import com.sporty.f1bets.generated.model.BetResponse;
import com.sporty.f1bets.generated.model.PlaceBetRequest;
import com.sporty.f1bets.shared.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BettingController implements BetsApi {

    private final PlaceBetService placeBetService;

    @Override
    public ResponseEntity<BetResponse> placeBet(PlaceBetRequest request) {
        PlacedBet placed =
                placeBetService.placeBet(request.getUserId(), request.getQuoteId(), Money.of(request.getAmountEur()));
        BetResponse response = new BetResponse()
                .betId(placed.betId())
                .status(placed.status().name())
                .newBalanceEur(placed.newBalance().toBigDecimal());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
