package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.EventOutcome;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Per-event transaction-scoped advisory lock. Both bet placement and settlement
 * acquire the same lock keyed by eventId, which serializes them and closes the
 * settle-vs-place race entirely (the lock releases at commit).
 *
 * <p>Implemented as a Spring Data query method: it runs on the current
 * transaction's {@code EntityManager}, so the lock is scoped to that
 * transaction. The {@code Repository<EventOutcome, Long>} type parameter only
 * anchors the repository; the query itself is a standalone native call.
 */
public interface EventLockRepository extends Repository<EventOutcome, Long> {

    @Query(value = "SELECT pg_advisory_xact_lock(:key)", nativeQuery = true)
    void lockEvent(@Param("key") long key);
}
