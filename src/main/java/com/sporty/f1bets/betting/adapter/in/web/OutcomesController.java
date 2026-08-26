package com.sporty.f1bets.betting.adapter.in.web;

import com.sporty.f1bets.betting.application.SettleOutcomeService;
import com.sporty.f1bets.betting.application.SettlementResult;
import com.sporty.f1bets.generated.api.OutcomesApi;
import com.sporty.f1bets.generated.model.OutcomeRequest;
import com.sporty.f1bets.generated.model.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OutcomesController implements OutcomesApi {

    private final SettleOutcomeService settleOutcomeService;

    @Override
    public ResponseEntity<SettlementResponse> settleOutcome(OutcomeRequest request) {
        SettlementResult result = settleOutcomeService.settle(request.getEventId(), request.getWinningDriverId());
        SettlementResponse response = new SettlementResponse()
                .eventId(result.eventId())
                .winningDriverId(result.winningDriverId())
                .settledBets(result.settledBets())
                .wonBets(result.wonBets())
                .lostBets(result.lostBets())
                .totalPaidOutEur(result.totalPaidOut().toBigDecimal());
        return ResponseEntity.ok(response);
    }
}
