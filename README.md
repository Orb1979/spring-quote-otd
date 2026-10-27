# quote-otd

Mindfulness **quote of the day** — a Spring Boot REST API. Viewers get a random
quote each time they ask; a quote is never repeated within a round, and the most
recent quotes are spaced out so a new round never starts with stale material.

## Requirements

- Java 21
- Docker (for the local PostgreSQL and for the test suite's Testcontainers)

## Run

```bash
docker compose up -d   # starts PostgreSQL 17 on localhost:5432
./gradlew bootRun       # starts the API on http://localhost:8080
```

Flyway migrates the schema automatically on startup.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Test

```bash
./gradlew test
```

The suite spins up its own PostgreSQL via Testcontainers — no local database
needed, but Docker must be running. (63 tests: MockMvc web slices, Mockito
service units, `@DataJpaTest` slices, and a `@SpringBootTest` end-to-end pass.)

## API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/quotes` | list all quotes |
| `POST` | `/api/quotes` | create quote — body `{"text": "..."}` → `201` + `Location` |
| `GET` | `/api/quotes/{id}` | one quote, `404` if unknown |
| `PUT` | `/api/quotes/{id}` | update quote text |
| `DELETE` | `/api/quotes/{id}` | `204`, `404` if unknown |
| `GET` | `/api/viewers` | list all viewers |
| `POST` | `/api/viewers` | create viewer — body `{"name": "..."}` |
| `GET` / `PUT` / `DELETE` | `/api/viewers/{id}` | viewer CRUD |
| `POST` | `/api/viewers/{id}/quotes/next` | show this viewer their next quote |
| `GET` | `/api/viewers/{id}/history` | the viewer's shown-quote history, newest first |

Errors are returned as RFC 7807 `application/problem+json`.

### Quickstart

```bash
curl -X POST localhost:8080/api/quotes -H 'Content-Type: application/json' -d '{"text":"Breathe in, breathe out."}'
curl -X POST localhost:8080/api/viewers -H 'Content-Type: application/json' -d '{"name":"Alice"}'
# use the viewer id from the response:
curl -X POST localhost:8080/api/viewers/<viewerId>/quotes/next
curl localhost:8080/api/viewers/<viewerId>/history
```

## How "next quote" works

- Let `N` be the number of quotes and `K = min(floor(N / 2), K_MAX)`, where
  `K_MAX` defaults to `50` and is configurable via `quote-otd.k-max`.
- Quotes are tracked per viewer in `quote_history` (`cycle`, `sequence`).
- A viewer never sees the same quote twice within one cycle; once all `N`
  quotes are shown, a new cycle starts with a reshuffled order.
- The last `K` shown quotes overall stay excluded — even across a cycle
  boundary — so a new cycle doesn't start with the round's old ending
  (`N = 2` → `K = 1` → the two quotes simply alternate; `N = 1` → `K = 0` →
  the single quote is just shown again).
- Old history is **pruned, never wiped**: only rows from previous cycles
  older than `K_MAX` sequences are deleted, so the distance rule keeps working.
- Concurrent "next" requests for the same viewer are serialized with a
  pessimistic lock on the viewer row.

## Configuration

`application.properties` connects to the compose database
(`localhost:5432/quote_db`, user `postgres`, password `password`) and sets
`quote-otd.k-max=50`.

## Troubleshooting

- `FATAL: database "quote_db" does not exist` — you are reusing a `postgres-db`
  container created before this compose file set `POSTGRES_DB: quote_db`
  (PostgreSQL only creates the database on first init of an empty volume).
  Either wipe the volume (`docker compose down -v` — removes data) or create it
  in place:
  ```bash
  docker exec postgres-db createdb -U postgres quote_db
  ```
