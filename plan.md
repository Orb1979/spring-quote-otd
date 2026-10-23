# quote-otd — TDD Build Plan

Mindfulness quote-of-the-day REST API, built one TDD cycle at a time (Red → Green → Refactor per step).

## Versions / Requirements
- Spring Boot `4.1.1`, Java 21, Gradle 9.8.1 wrapper
- Flyway, PostgreSQL, Lombok
- springdoc-openapi-starter-webmvc-ui
- Tests run against **Testcontainers** Postgres (`postgres:17`)
- `docker-compose.yml` (already in root) is for manual runs / dev

## Domain model
- **quote** (id uuid, text)
- **viewer** (id uuid, name)
- **quote_history** (viewer_id FK, quote_id FK, cycle, sequence, shown_at) — junction table

## Data model (PostgreSQL)
- quote(id uuid PK, text text NOT NULL UNIQUE)
- viewer(id uuid PK, name text NOT NULL)
- quote_history(viewer_id FK, quote_id FK, cycle int, sequence int, shown_at timestamptz)
    - PRIMARY KEY (viewer_id, quote_id, cycle)
    - UNIQUE (viewer_id, sequence)
    - INDEX (viewer_id, sequence DESC)
    - FKs use ON DELETE CASCADE

Cardinality:
- viewer 1--\* quoteHistory (one viewer has many history rows; new viewer has zero)
- quote 1--\* quoteHistory (one quote has many history rows; unseen quote has zero)
- viewer \*--\* quote — not direct, goes through quoteHistory: viewer 1--\* quoteHistory \*--1 quote

## "Next quote" logic
Each viewer gets quotes in random order, but never the same one twice until they’ve seen all of them. 
Once they’ve seen every quote, a new round starts and the order is reshuffled.

To make the start of a new round feel fresh, the app also avoids the most recently shown quotes 
(up to 50, or half the list if you have fewer quotes). 
That way, the last quote of one round can’t come back right at the start of the next.

Each time a quote is shown, the app records it in the viewer’s history. Old history that is no longer needed 
is deleted, so it never grows forever. Only the current round and the most recent 50 quotes are kept.

If you add or remove quotes, nothing breaks. A new quote can be shown right away, and a removed one simply
disappears from the history.
```
- `quote_history(viewer_id, quote_id, cycle, sequence, shown_at)`
    - PK `(viewer_id, quote_id, cycle)`, UNIQUE `(viewer_id, sequence)`
    - `sequence` grows per viewer forever; `cycle` starts at 1
- `N` = number of quotes, 
- `K = min(floor(N / 2), K_MAX)`, `K_MAX = 50` (config)

## Algorithm (one transaction, per viewer)
1. If `N = 0`, return an error.
2. Read the viewer's last history row to get the current `cycle` and `sequence` (defaults: 1 and 0).
3. If every current quote is already in this cycle's history, set `cycle = cycle + 1`. Delete nothing.
4. Eligible quotes = not in the current cycle AND not in the viewer's last `K` rows (across all cycles).
5. Pick one eligible quote at random.
6. Insert `(viewer, quote, cycle, sequence + 1, now())`.
7. Prune: delete the viewer's rows where `cycle < current_cycle` AND `sequence <= new_sequence - K_MAX`. Always use `K_MAX`, never `K`.
8. Return the quote.

## Notes
- The "last K" rule only matters at the start of a new cycle. It stops the end of the old cycle from repeating.
- Lock the viewer row (or retry on a unique violation) to handle concurrent requests.
```

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
