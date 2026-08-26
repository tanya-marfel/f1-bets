# F1 Betting Service

A Formula 1 betting backend exposing a REST API to **list events**, **place bets**, and **settle event outcomes**. Events and drivers come from the open‑source [openf1.org](https://openf1.org) API, kept behind a port so the provider can be swapped without touching the rest of the system.

---

## Tech stack

- **Java 25**, **Spring Boot 4.1**
- **PostgreSQL** (via Docker) with **Flyway** migrations
- **OpenAPI contract-first** — request/response DTOs + API interfaces generated from [`f1-bets.yaml`](src/main/resources/openapi/f1-bets.yaml) by `openapi-generator`; **springdoc** serves Swagger UI + `/v3/api-docs`
- **Testcontainers** for integration tests, **WireMock** for the HTTP‑client test
- **JaCoCo** with an 80% line‑coverage gate
- **Gradle** (Kotlin DSL)

## Architecture

Pragmatic **hexagonal / ports & adapters**, organised **package‑by‑feature**:

```
com.sporty.f1bets
├── events        # list events + issue odds quotes
│   ├── domain            (Event, Driver, DriverMarketEntry)
│   ├── application       (ListEventsService, EventProviderPort  ← the only outbound port)
│   └── adapter
│       ├── in/web        (EventsController implements the generated EventsApi)
│       └── out/openf1    (OpenF1EventProvider using RestClient)
├── betting       # place bets, wallet, settlement
│   ├── domain            (User, Bet, BetStatus, EventOutcome — domain == JPA entity)
│   ├── application       (PlaceBetService, SettleOutcomeService, WalletService, repositories, advisory lock)
│   └── adapter/in/web    (BettingController/OutcomesController implement generated BetsApi/OutcomesApi)
├── shared        # Money, OddsQuote (issued in events, consumed in betting), OddsGenerator, RFC 7807 handler
└── config        # Clock, OddsGenerator and timeout‑configured RestClient beans
```

Only `EventProviderPort` is abstracted as a port — it’s the only boundary the brief actually motivates (“we will add new providers”). `betting` never depends on `events`; the two features communicate only through the shared `OddsQuote`.

---

## How the odds / quote model works

The brief says odds are a random `2`, `3` or `4`, re‑rolled on every listing. To keep the **server authoritative** over the price a user bets at, listing an event **issues a single‑use, expiring quote** per driver:

1. `GET /events` → for every driver the server generates odds and returns a `quoteId` + `quoteExpiresAt`.
2. `POST /bets` references a `quoteId`. The server resolves it (404 if unknown, 410 if expired), snapshots the odds onto the bet, and debits the balance. A `UNIQUE` constraint on `bets.quote_id` makes the quote single‑use.

Because the quote is minted from the provider’s real market, membership is validated at issuance — placement needs no provider call.

---

## Running the service

### Prerequisites
- **JDK 25**
- **Docker** running (Docker Desktop, Colima, Rancher, …)

### Start it
```bash
./gradlew bootRun
```
Spring Boot’s Docker Compose support automatically starts the Postgres container defined in [`compose.yaml`](compose.yaml) (with a `pg_isready` healthcheck) and runs the Flyway migrations. The API is then available at `http://localhost:8080`.

### Run the whole stack in Docker

Run the **app itself in a container** (Postgres + app, all in Docker):
```bash
make up          # builds the jar, then `docker compose --profile app up --build`
```
Without make: `./gradlew bootJar && docker compose --profile app up --build`.

The `app` service sits behind a Compose **profile**, so plain `./gradlew bootRun` (or `make run`) still starts only Postgres. The container waits for Postgres to be healthy, runs Flyway, and exposes the API on `http://localhost:8080`. (The jar is built on the host/CI and copied into a slim `eclipse-temurin:25-jre` image.)

Run `make help` for all targets — `build`, `test`, `run`, `up`, `up-detached`, `down`, `logs`, `clean`.

A demo user is seeded at registration with **id `1`** and **100 EUR**.

Health probe: `GET http://localhost:8080/actuator/health`.

Interactive API docs: **Swagger UI** at `http://localhost:8080/swagger-ui/index.html`, and the **OpenAPI 3.1** document at `http://localhost:8080/v3/api-docs`.

The API is **contract-first**: [`f1-bets.yaml`](src/main/resources/openapi/f1-bets.yaml) is the source of truth. `openapi-generator` produces the DTOs and per-feature API interfaces (`EventsApi`/`BetsApi`/`OutcomesApi`) from it at build time; the controllers implement those interfaces and map to/from the domain. Editing the API means editing the spec first.

---

## API

All endpoints are under `/api/v1`.

### 1. List events
```bash
curl "http://localhost:8080/api/v1/events?year=2023&country=Italy&sessionType=Race"
```
Filters (`sessionType`, `year`, `country`) are all optional. Response (truncated):
```json
[
  {
    "eventId": 9158,
    "sessionType": "Race",
    "year": 2023,
    "country": "Italy",
    "sessionName": "Race",
    "drivers": [
      { "driverNumber": 1, "fullName": "Max VERSTAPPEN", "odds": 3,
        "quoteId": "0f0e...c1", "quoteExpiresAt": "2026-08-25T18:40:00Z" }
    ]
  }
]
```

### 2. Place a bet
```bash
curl -X POST http://localhost:8080/api/v1/bets \
  -H 'Content-Type: application/json' \
  -d '{"userId": 1, "quoteId": "0f0e...c1", "amountEur": 25.00}'
```
`201 Created`:
```json
{ "betId": 1, "status": "PENDING", "newBalanceEur": 75.00 }
```

### 3. Settle an event outcome
```bash
curl -X POST http://localhost:8080/api/v1/outcomes \
  -H 'Content-Type: application/json' \
  -d '{"eventId": 9158, "winningDriverId": 1}'
```
`200 OK`:
```json
{ "eventId": 9158, "winningDriverId": 1, "settledBets": 3, "wonBets": 1, "lostBets": 2, "totalPaidOutEur": 75.00 }
```

### Errors
Errors use **RFC 7807** `application/problem+json`, e.g. a 404:
```json
{ "type": "https://f1-bets/errors/quote-not-found", "title": "Quote not found",
  "status": 404, "detail": "Quote not found: 0f0e...c1", "quoteId": "0f0e...c1" }
```
| Situation | Status |
|---|---|
| Validation error | 400 |
| Quote / user not found | 404 |
| Insufficient funds, quote already used, event already settled, concurrent modification | 409 |
| Quote expired | 410 |
| Invalid bet amount | 400 |
| openf1.org unavailable / timed out | 502 |

---

## Testing

```bash
./gradlew check              # Small + Medium tests + 80% coverage gate
./gradlew test               # Small (unit) tests only — fast, no Docker (alias: smallTest)
./gradlew mediumTest         # Medium tests (Testcontainers + WireMock) — requires Docker
./gradlew jacocoTestReport   # HTML coverage at build/reports/jacoco/test/html/index.html
```

Tests follow Google's **test‑size** model, selected by JUnit `@Tag` over a single `test` source set (one classpath; sizes are separate *tasks*, not source sets):
- **Small** (`@Small`) — pure domain/application unit tests, no Spring, no I/O; run by `test` / `smallTest`.
- **Medium** (`@Medium`) — full‑stack tests on Testcontainers Postgres, plus the openf1 adapter against WireMock; run by `mediumTest`.

`check` runs both sizes and enforces the coverage gate.

Highlights: an end‑to‑end flow (list → bet → settle), a **no‑bet‑after‑settlement** guard, an RFC 7807 body‑shape check, and three **concurrency** tests (optimistic‑lock balance protection, single‑use quote, and a settle‑vs‑place invariant proving no bet lingers `PENDING` on a settled event).

Current: **41 tests, ~92% line coverage.**

---

## Design notes

- **Idempotent settlement** — `event_outcomes.event_id` is the primary key; settlement checks existence under a lock and refuses to settle twice (409).
- **No bet after settlement** — placement and settlement both take a per‑event **Postgres advisory lock** (`pg_advisory_xact_lock`), which serialises them and fully closes the settle‑vs‑place race.
- **Optimistic locking** — `User.balance` is guarded by a `@Version` column; concurrent debits are safe (loser gets 409).
- **Money** — `BigDecimal`, scale 2, `HALF_EVEN`, stored as `NUMERIC(19,2)`; payout = stake × odds.
- **Provider decoupling** — the only outbound port is `EventProviderPort`; the `RestClient` has 5s connect/read timeouts and upstream failures map to **502**.
- **Transactions** — `placeBet` and `settle` are each a single `@Transactional` unit.

## What I skipped and why

- **No authentication / single seeded user** — out of scope; a user id is passed as a parameter per the brief.
- **No `Idempotency-Key` on `POST /bets`** — single‑use quotes already give per‑quote idempotency; a request header would add per‑request idempotency and is the natural next step.
- **No quote purging** — expired quotes are inert (filtered at placement); production would schedule a cleanup / partition the table.
- **No upper bet limit beyond balance** — production would add a configurable max.
- **No provider caching / circuit breaker** — fine at low RPS; would add Resilience4j in production.
- **Domain classes double as JPA entities** in `betting** — a deliberate simplification; a larger system would introduce a mapper layer to keep the domain framework‑free.
- **ArchUnit boundary test omitted** — its bundled ASM couldn’t parse Java 25 bytecode; the `betting`‑↛‑`events` boundary is instead guaranteed by construction (verified: no `betting` class imports `events`).

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `./gradlew check jacocoTestReport` on Temurin 25 for every push to `main` and every PR, and uploads the test + coverage reports as artifacts. Testcontainers uses the runner’s Docker daemon.









