# F1 Betting Service

A backend service for betting on Formula 1 race winners. It exposes a small REST API to **list events**, **place bets**
on a driver to win, and **settle event outcomes** — paying out winners and closing losing bets in a single transaction.

Event and driver data comes from the public [openf1.org](https://openf1.org) API. Odds are issued by the service itself
as **single-use, expiring quotes**, so the server always stays authoritative over the price a bet is accepted at.

---

## Table of contents

- [Quick start](#quick-start)
- [Concepts](#concepts)
- [API reference](#api-reference)
- [Errors](#errors)
- [Configuration](#configuration)
- [Data model](#data-model)
- [Architecture](#architecture)
- [Development](#development)
- [Testing](#testing)
- [Operations](#operations)
- [Roadmap and known limitations](#roadmap-and-known-limitations)

---

## Quick start

### Prerequisites

- **JDK 25** — for local development and the test suite
- **Docker** running (Docker Desktop, Colima, Rancher Desktop, …)

Running the full stack with `make up` needs **only Docker** — the jar is built inside the image.

### Run locally

```bash
./gradlew bootRun      # or: make run
```

Spring Boot's Docker Compose support starts the PostgreSQL container defined in [`compose.yaml`](compose.yaml) (with a
`pg_isready` healthcheck), Flyway applies the migrations, and the API comes up on `http://localhost:8080`.

### Run the full stack in Docker

```bash
make up                # builds the image from source, then starts Postgres + the app in Docker
```

Without `make`: `docker compose --profile app up --build`. No prior Gradle build is needed — this path requires only
Docker.

The `app` service sits behind a Compose **profile**, so `./gradlew bootRun` still starts only Postgres. The container
waits for Postgres to become healthy, runs Flyway, and exposes the API on `http://localhost:8080`.

[`Dockerfile`](Dockerfile) is a **multi-stage** build: the jar is compiled inside a `gradle:…-jdk25-corretto-al2023`
stage, then its Spring Boot layers are extracted into an `amazoncorretto:25-al2023-headless` runtime image running as a
non-root user (uid `10001`). Dependencies and application code land in separate layers, so a code change rebuilds only
the small final layer.

Run `make help` for all targets: `build`, `test`, `run`, `check-versions`, `up`, `up-detached`, `down`, `logs`,
`clean`.

### First request

A demo user is seeded by migration with **id `1`** and a balance of **100.00 EUR**.

```bash
# 1. list events and grab a quoteId
curl "http://localhost:8080/api/v1/events?year=2023&country=Italy&sessionType=Race"

# 2. place a bet with that quote
curl -X POST http://localhost:8080/api/v1/bets \
  -H 'Content-Type: application/json' \
  -d '{"userId": 1, "quoteId": "<quoteId>", "amountEur": 25.00}'

# 3. settle the event
curl -X POST http://localhost:8080/api/v1/outcomes \
  -H 'Content-Type: application/json' \
  -d '{"eventId": 9158, "winningDriverId": 1}'
```

### Interactive docs

- **Swagger UI** — `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI document** — `http://localhost:8080/v3/api-docs`
- **Health probe** — `http://localhost:8080/actuator/health`

---

## Concepts

### Odds quotes

Odds are issued per driver, per listing. Listing an event mints a **single-use quote** for every driver in the market:

1. `GET /api/v1/events` generates odds for each driver and returns a `quoteId` plus a `quoteExpiresAt` timestamp (TTL is
   configurable, default **5 minutes**).
2. `POST /api/v1/bets` references a `quoteId`. The service resolves it — `404` if unknown, `410` if expired — snapshots
   the odds onto the bet, and debits the user's balance.
3. A `UNIQUE` constraint on `bets.quote_id` makes each quote usable exactly once; a second attempt gets `409`.

Because a quote is minted from the provider's real market, driver membership is validated at issuance — placing a bet
requires no upstream call and stays fast and available even if openf1.org is degraded.

### Settlement

`POST /api/v1/outcomes` closes an event:

- The event id is the **primary key** of `event_outcomes`, so settlement is idempotent by construction — a second
  attempt is rejected with `409`.
- Every `PENDING` bet on the event is marked `WON` or `LOST`. Winners are credited `stake × odds_at_placement`.
- The response summarises what happened: bets settled, won, lost, and total paid out.

### Concurrency guarantees

| Race                                           | Guard                                                                                                                   |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Two bets consuming the same quote              | `UNIQUE (bets.quote_id)` → the loser gets `409 Quote already used`                                                      |
| Concurrent debits of the same balance          | `@Version` optimistic lock on `users` → the loser gets `409 Concurrent modification`                                    |
| A bet landing while the event is being settled | Per-event **Postgres advisory lock** (`pg_advisory_xact_lock`) taken by both placement and settlement, serialising them |
| Settling the same event twice                  | `event_outcomes.event_id` primary key + existence check under the lock                                                  |

Placement and settlement are each a single `@Transactional` unit, so no bet can linger `PENDING` on an already-settled
event.

### Money

All amounts are `BigDecimal` with **scale 2** and **`HALF_EVEN`** rounding, persisted as `NUMERIC(19,2)`. The `Money`
value object is immutable and is the only way amounts move through the system. Payout = stake × odds.

---

## API reference

All endpoints live under `/api/v1`. Request and response bodies are `application/json`; errors are
`application/problem+json`.

### `GET /api/v1/events`

List events with a per-driver odds market and freshly issued quotes.

| Query param   | Type    | Required | Description                           |
|---------------|---------|----------|---------------------------------------|
| `sessionType` | string  | no       | e.g. `Race`, `Qualifying`, `Practice` |
| `year`        | integer | no       | Season, e.g. `2023`                   |
| `country`     | string  | no       | Country name, e.g. `Italy`            |

All filters are optional and are passed through to the provider.

**`200 OK`** (truncated):

```json
[
  {
    "eventId": 9158,
    "sessionType": "Race",
    "year": 2023,
    "country": "Italy",
    "sessionName": "Race",
    "drivers": [
      {
        "driverNumber": 1,
        "fullName": "Max VERSTAPPEN",
        "odds": 3,
        "quoteId": "0f0e2c6a-1f5b-4f0b-9a51-2f7a1c9d10c1",
        "quoteExpiresAt": "2026-08-25T18:40:00Z"
      }
    ]
  }
]
```

Possible errors: `502` when openf1.org is unavailable or times out.

### `POST /api/v1/bets`

Place a bet by consuming a quote.

```json
{
  "userId": 1,
  "quoteId": "0f0e2c6a-1f5b-4f0b-9a51-2f7a1c9d10c1",
  "amountEur": 25.00
}
```

| Field       | Type   | Required | Constraints                           |
|-------------|--------|----------|---------------------------------------|
| `userId`    | int64  | yes      | must exist                            |
| `quoteId`   | uuid   | yes      | must be known, unexpired and unused   |
| `amountEur` | number | yes      | `>= 0.01` and `<=` the user's balance |

**`201 Created`**:

```json
{
  "betId": 1,
  "status": "PENDING",
  "newBalanceEur": 75.00
}
```

Possible errors: `400`, `404`, `409`, `410` (see [Errors](#errors)).

### `POST /api/v1/outcomes`

Settle a finished event.

```json
{
  "eventId": 9158,
  "winningDriverId": 1
}
```

**`200 OK`**:

```json
{
  "eventId": 9158,
  "winningDriverId": 1,
  "settledBets": 3,
  "wonBets": 1,
  "lostBets": 2,
  "totalPaidOutEur": 75.00
}
```

Possible errors: `400`, `409`.

---

## Errors

Every error is an **RFC 7807** problem document served as `application/problem+json`, with `type`, `title`, `status`,
`detail` and — where relevant — the extension members `quoteId`, `eventId` or `userId`.

```json
{
  "type": "https://f1-bets/errors/quote-not-found",
  "title": "Quote not found",
  "status": 404,
  "detail": "Quote not found: 0f0e2c6a-1f5b-4f0b-9a51-2f7a1c9d10c1",
  "quoteId": "0f0e2c6a-1f5b-4f0b-9a51-2f7a1c9d10c1"
}
```

| `type` suffix             | Title                      | Status | When                                                       |
|---------------------------|----------------------------|--------|------------------------------------------------------------|
| `invalid-bet-amount`      | Invalid bet amount         | `400`  | Stake is zero or negative                                  |
| *(framework)*             | Bad Request                | `400`  | Request body fails bean validation                         |
| `quote-not-found`         | Quote not found            | `404`  | `quoteId` is unknown                                       |
| `user-not-found`          | User not found             | `404`  | `userId` is unknown                                        |
| `insufficient-funds`      | Insufficient funds         | `409`  | Stake exceeds the balance                                  |
| `quote-already-used`      | Quote already used         | `409`  | The quote was already consumed by a bet                    |
| `event-already-settled`   | Event already settled      | `409`  | Settling twice, or betting on a settled event              |
| `concurrent-modification` | Concurrent modification    | `409`  | Optimistic lock lost — safe to retry                       |
| `quote-expired`           | Quote expired              | `410`  | The quote's TTL has elapsed — list again for a fresh price |
| `provider-unavailable`    | Event provider unavailable | `502`  | openf1.org errored or timed out                            |

All `type` values are prefixed with `https://f1-bets/errors/`.

---

## Configuration

Defaults live in [`application.properties`](src/main/resources/application.properties). Every key can be overridden with
an environment variable using Spring's relaxed binding (`OPENF1_BASE_URL`, `QUOTE_TTL`, …).

| Property                                    | Default                     | Description                                         |
|---------------------------------------------|-----------------------------|-----------------------------------------------------|
| `openf1.base-url`                           | `https://api.openf1.org/v1` | Upstream event provider base URL                    |
| `openf1.connect-timeout`                    | `5s`                        | HTTP connect timeout to the provider                |
| `openf1.read-timeout`                       | `5s`                        | HTTP read timeout to the provider                   |
| `quote.ttl`                                 | `5m`                        | How long an issued odds quote stays valid           |
| `spring.datasource.url`                     | from Compose                | JDBC URL; set explicitly when not using Compose     |
| `spring.datasource.username` / `.password`  | from Compose                | Database credentials                                |
| `spring.jpa.hibernate.ddl-auto`             | `validate`                  | Flyway owns the schema; Hibernate only validates it |
| `spring.flyway.enabled`                     | `true`                      | Run migrations on startup                           |
| `management.endpoints.web.exposure.include` | `health,info`               | Exposed actuator endpoints                          |

In Docker the app service is wired with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD` (see [`compose.yaml`](compose.yaml)).

---

## Data model

The schema is owned by Flyway ([`src/main/resources/db/migration`](src/main/resources/db/migration)); Hibernate is set
to `validate` and never mutates it.

| Table            | Key columns                                                                                                                                                  | Notes                                              |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| `users`          | `id` PK, `balance_eur NUMERIC(19,2)`, `version`                                                                                                              | `version` drives optimistic locking on the balance |
| `odds_quotes`    | `quote_id UUID` PK, `event_id`, `driver_id`, `odds`, `created_at`, `expires_at`                                                                              | Indexed on `event_id`                              |
| `bets`           | `id` PK, `user_id` FK → `users`, `event_id`, `driver_id`, `amount_eur`, `odds_at_placement`, `status`, `quote_id` **UNIQUE** FK → `odds_quotes`, `placed_at` | Indexed on `(event_id, status)` and `user_id`      |
| `event_outcomes` | `event_id` PK, `winning_driver_id`, `settled_at`                                                                                                             | The PK is what makes settlement idempotent         |

`V2__seed_users.sql` seeds the demo user (`id = 1`, `100.00 EUR`) and advances the identity sequence.

---

## Architecture

Pragmatic **hexagonal / ports & adapters**, organised **package-by-feature**:

```
com.sporty.f1bets
├── events        # list events + issue odds quotes
│   ├── domain            (Event, Driver, DriverMarketEntry)
│   ├── application       (ListEventsService, EventProviderPort ← the only outbound port)
│   └── adapter
│       ├── in/web        (EventsController implements the generated EventsApi)
│       └── out/openf1    (OpenF1EventProvider using RestClient)
├── betting       # place bets, wallet, settlement
│   ├── domain            (User, Bet, BetStatus, EventOutcome — domain == JPA entity)
│   ├── application       (PlaceBetService, SettleOutcomeService, WalletService, repositories, advisory lock)
│   └── adapter/in/web    (BettingController / OutcomesController implement the generated BetsApi / OutcomesApi)
├── shared        # Money, OddsQuote (issued in events, consumed in betting), OddsGenerator, RFC 7807 handler
└── config        # Clock, OddsGenerator and timeout-configured RestClient beans
```

`EventProviderPort` is the only abstracted outbound port — it is the boundary genuinely expected to change, so swapping
openf1.org for another data source touches one adapter and nothing else. `OpenF1EventProvider` translates transport
failures into `ProviderUnavailableException` (→ `502`) and de-duplicates the driver rows openf1 sometimes repeats within
a session.

`betting` never depends on `events`; the two features communicate only through the shared `OddsQuote`. Domain classes in
`betting` double as JPA entities — a deliberate simplification at this size; a larger system would introduce a mapper
layer to keep the domain framework-free.

### Contract-first API

[`f1-bets.yaml`](src/main/resources/openapi/f1-bets.yaml) is the source of truth. `openapi-generator` produces the DTOs
and per-feature API interfaces (`EventsApi`, `BetsApi`, `OutcomesApi`) at build time; controllers implement those
interfaces and map to and from the domain. **Change the spec first** — the compiler will then point at every controller
that needs updating. `springdoc` serves Swagger UI and `/v3/api-docs` at runtime.

---

## Development

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL 16 · Flyway · Gradle (Kotlin DSL) · Lombok · springdoc ·
Testcontainers · WireMock · JaCoCo · Spotless.

```bash
./gradlew build          # compile + generate API sources + jar
./gradlew bootJar        # executable jar   (alias: make build)
./gradlew check          # format check + all tests + coverage gate (alias: make test)
./gradlew bootRun        # run locally      (alias: make run)
./gradlew spotlessApply  # reformat the code in place
./gradlew clean          # clean build output
```

Generated OpenAPI sources land in `build/generated/openapi` and are added to the main source set automatically — never
edit them by hand.

A database change means adding a new `V<n>__description.sql` migration; applied migrations are immutable.

### Dependency versions

Every external version — plugins, libraries, the JDK — is declared in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml); the build scripts hold no hardcoded versions. Where the Spring
Boot BOM already manages a coordinate (the starters, Flyway, Testcontainers), the catalog entry deliberately omits the
version so the BOM stays authoritative and modules cannot drift apart.

The `Dockerfile` pins its JDK and Gradle through build `ARG`s; `make check-versions` cross-checks them against the
catalog and the Gradle wrapper and fails if they ever drift.

### Code style

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using palantir-java-format. `spotlessCheck`
is wired into `check`, so CI fails on unformatted code — run `./gradlew spotlessApply` to fix. Generated sources are
excluded.

---

## Testing

Tests follow Google's **test-size** model, selected by JUnit `@Tag` over a single `test` source set — one classpath,
with sizes as separate Gradle *tasks*:

```bash
./gradlew test               # Small (unit) tests only — fast, no Docker (alias: smallTest)
./gradlew mediumTest         # Medium tests — Testcontainers + WireMock, requires Docker
./gradlew check              # Small + Medium + the 80% line-coverage gate
./gradlew jacocoTestReport   # HTML coverage at build/reports/jacoco/test/html/index.html
```

- **Small** (`@Small`) — pure domain and application unit tests: no Spring context, no I/O.
- **Medium** (`@Medium`) — full-stack tests against a Testcontainers Postgres, plus the openf1 adapter driven against
  WireMock.

Coverage is enforced at **80% lines** by `jacocoTestCoverageVerification`, wired into `check`. Generated code,
configuration, DTOs and the error package are excluded from the metric.

Notable scenarios covered: the end-to-end flow (list → bet → settle), the no-bet-after-settlement guard, the RFC 7807
body shape, and three concurrency tests (optimistic-lock balance protection, single-use quotes, and a settle-vs-place
invariant proving no bet lingers `PENDING` on a settled event).

---

## Operations

- **Health** — `GET /actuator/health` (also the container healthcheck) and `GET /actuator/info`. Only these two
  endpoints are exposed.
- **Migrations** — applied by Flyway at startup; the app fails fast if the schema does not match the entities.
- **Upstream dependency** — openf1.org is called only by `GET /events`, with 5s connect/read timeouts. Failures surface
  as `502` and never corrupt local state; betting and settlement remain fully available.
- **Retryable errors** — `409 Concurrent modification` is a lost optimistic lock and is safe for clients to retry.
  `410 Quote expired` requires re-listing to obtain a fresh price.
- **Scaling** — the service is stateless; all coordination happens in Postgres (advisory locks, unique constraints,
  optimistic locking), so instances scale horizontally.

---

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `./gradlew check jacocoTestReport` on Temurin 25 for every
push to `main` and every pull request, then uploads the test and coverage reports as build artifacts. Because
`spotlessCheck` and the coverage gate are both wired into `check`, a single command enforces formatting, tests and
coverage. Testcontainers uses the runner's Docker daemon, so the Medium tests need no extra setup, and in-progress runs
for the same ref are cancelled automatically.

---

## Roadmap and known limitations

- **No authentication** — the user is identified by an id in the request body. A real deployment would sit behind an
  auth layer and derive the user from the token.
- **No `Idempotency-Key` on `POST /bets`** — single-use quotes already provide per-quote idempotency; a request-level
  idempotency key is the natural next step.
- **Expired quotes are never purged** — they are inert (rejected at placement), but production should schedule a cleanup
  job or partition the table.
- **No upper bet limit beyond the balance** — a configurable maximum stake and responsible-gambling limits would be
  required in production.
- **No provider caching or circuit breaker** — acceptable at low request rates; Resilience4j plus a short-lived cache of
  the openf1 market is the obvious hardening step.
- **Single currency (EUR)** — currency is implicit in `Money` and in the schema.

