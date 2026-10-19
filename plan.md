# quote-otd — TDD Build Plan

Mindfulness quote-of-the-day REST API, built one TDD cycle at a time (Red → Green → Refactor per step).

## Versions / Requirements
- Spring Boot `4.1.1`, Java 21, Gradle 9.8.1 wrapper
- Flyway, PostgreSQL, Lombok
- `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`
- `implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1'`
- Tests run against **Testcontainers** Postgres (`postgres:17`) — self-contained `./gradlew test`, no manual docker-compose startup needed
- `docker-compose.yml` (already in root) is for manual runs / dev

## Domain model
- **quote** (id uuid, text)
- **viewer** (id uuid, name)
- **quote_history** (viewer_id FK, quote_id FK, cycle, sequence, shown_at) — junction table

Cardinality:
- viewer 1--\* quoteHistory (one viewer has many history rows; new viewer has zero)
- quote 1--\* quoteHistory (one quote has many history rows; unseen quote has zero)
- viewer \*--\* quote — not direct, goes through quoteHistory: viewer 1--\* quoteHistory \*--1 quote

## "Next quote" logic
```
K = min(floor(N / 2), K_max), K_max = 25
n = 10  → k = 5
n = 200 → k = 25
n = 1   → k = 0 (just show it)
n = 2   → k = 1 (they alternate)
```
1. Find viewer's current cycle (highest in quote_history, or 1 if empty) and last sequence.
2. If current cycle already contains all N quotes → start new cycle (cycle + 1). Nothing is wiped.
3. Eligible quotes = not in current cycle AND not among the last K shown overall (by sequence, across cycles).
4. Pick one eligible quote at random → insert (viewer_id, quote_id, cycle, sequence + 1).

History pruning (never wiped entirely; only needs current-cycle rows + last K rows overall):
```sql
DELETE FROM quote_history
WHERE viewer_id = :viewer
  AND cycle < :current_cycle
  AND sequence <= :current_sequence - :k;   -- k = K_max
```

## Steps

| Step | Scope | Tests written first |
|---|---|---|
| **0** | Baseline: `build.gradle` deps (springdoc 3.1.1, Testcontainers 1.21.4), datasource/Flyway config, green `contextLoads` | existing smoke test |
| **1** | `V1__init.sql` (exact schema: viewer, quote, quote_history, idx_quote_history_recent) | Flyway/schema test against Testcontainers Postgres |
| **2** | Quote CRUD (`/api/quotes`) | `@WebMvcTest` red → service unit tests → entity/repo → green |
| **3** | Viewer CRUD (`/api/viewers`) | same pattern |
| **4** | `QuoteHistory` entity (composite PK `(viewer_id, quote_id, cycle)`) + repositories incl. prune query | repository tests on Testcontainers Postgres |
| **5** | Next-quote service: `k = min(floor(n/2), 25)`, cycle resolution, rollover, eligibility (exclude current cycle + last K overall), random pick + insert | pure-logic unit tests for all edge cases (n=1→0, n=2→1, n=10→5, n=200→25), then repo verification |
| **6** | Endpoints: `POST /api/viewers/{id}/quotes/next`, `GET /api/viewers/{id}/history` | MockMvc tests |
| **7** | README (run/test instructions, Swagger URL), prune-after-insert wiring, error handling polish | final test pass |

**Progress:** Step 0 complete ✅ (baseline: datasource + Flyway config, Testcontainers wired, springdoc 3.1.1, smoke test green). Step 1 next.

**Process rule:** one TDD cycle per reply, stop after each phase (Red / Green / Refactor) for feedback.
