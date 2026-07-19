# Administrative Area API

A read-optimised reference-data service over Uganda's six-level administrative hierarchy:

```text
Region › Sub-Region › Local Government › County › Sub-County › Parish
```

It is **read-almost-only** — the hierarchy changes maybe monthly — and is built to be integrated by
many services and browsers at once, serving those reads very fast. Spring Boot 3.2.3 on Java 21, with
virtual threads enabled.

---

## What it does

- **Search and fetch** areas at any level, with dynamic filtering, sorting and paging. Every item
  carries its **full ancestry** (a parish nests its sub-county → … → region), or just its immediate
  parent's code when you ask for `?view=flat`.
- **Create / update / delete** with parent-scoped duplicate detection and soft delete (an area with
  children cannot be removed).
- **Re-parent** an area, moving its whole sub-hierarchy in one operation.

Everything is served behind a **two-tier cache** (see below), so the common read never touches
Postgres and the service stays up — degraded to database reads — when Redis is down.

---

## API

Base path: `/api/v1/administrative-areas`. All responses are JSON envelopes
(`{ data, message, status, … }`); errors are RFC-7807 `application/problem+json`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/search` | Page through a level; filter, sort, `?view=flat` |
| `GET` | `/{code}` | One area by its code, with full ancestry |
| `POST` | `/` | Create an area |
| `PUT` | `/{code}` | Update or re-parent |
| `DELETE` | `/{code}` | Soft delete (fails if it has children) |

### Searching

`GET /search` takes `type` (the level), 1-based `page`, `size` (default 50, max 5000, clamped),
`sortBy`, `sortDirection`, `partOf` (a parent's code, to list its children), and `view`.

Advanced filters use the grammar `field:OPERATOR=value` as query keys, e.g.
`name:CONTAINS=kampala`. Operators: `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `NOT_CONTAINS`, `GT`, `GTE`,
`LT`, `LTE`, `IN`, `IS_NULL`, `IS_NOT_NULL`.

`IS_NULL` / `IS_NOT_NULL` ignore the supplied value, but one must still be sent — a filter with a
blank value is dropped before it reaches the specification. Write `parent:IS_NULL=true`, not
`parent:IS_NULL=`.

**`?view=flat`** returns each item as its own fields plus `partOfCode` (the immediate parent's code)
instead of the nested ancestry. It is much smaller and skips the joins up the hierarchy — use it for
large pulls like the map or a picker, then follow `partOfCode` with another search if you need the
parent. Any value other than `flat` returns the default nested shape.

```bash
# nested (default)
curl "http://localhost:4401/api/v1/administrative-areas/search?type=SUBCOUNTY&page=1&size=25&sortBy=name&sortDirection=asc"

# flat
curl "http://localhost:4401/api/v1/administrative-areas/search?type=SUBCOUNTY&page=1&size=25&view=flat"
```

Interactive docs (when enabled): **[Swagger UI](http://localhost:4401/swagger-ui.html)**.

---

## Caching

Reads go through a two-tier cache, one region per level per shape (12 in all):

```text
Caffeine L1 (in-heap, per-pod, ~100 ns)
   │ miss
Redis L2 (shared across pods, ~1–3 ms)   ──▶ on hit, populate L1
   │ miss
Postgres
```

- **Scoped, cascading eviction.** A write evicts only the level written *and the levels below it*
  (a parish embeds its region, so renaming a region invalidates every parish — but not vice versa).
  Eviction fires `afterCommit`, never before, so a concurrent reader cannot re-cache pre-commit data.
- **Cross-pod invalidation** over Redis pub/sub: the writing pod clears the shared L2 and tells every
  other pod to drop its in-heap copy. The L1 TTL (5 min) is the backstop if a message is missed.
- **Resilient to Redis failure.** L2 errors degrade to a cache miss (served from Postgres), fast:
  Lettuce is set to reject commands when disconnected and time out in 250 ms rather than hang.
- **Conditional requests.** `GET /search` and `GET /{code}` carry a version-based **ETag**; a repeat
  request with `If-None-Match` short-circuits to `304` before any query, mapping or serialization.
  When Redis is unreachable the ETag is withheld rather than risk a stale `304`.
- **Warmed on start**, before the readiness probe flips, so pods take traffic already hot.

### Evicting the cache (operators)

`AreaCacheAdminEndpoint`, on the management port. Unlike Boot's `/actuator/caches` it also bumps the
ETag versions and broadcasts to other pods — a correct cluster-wide wipe needs all three.

```bash
curl -X DELETE http://localhost:5401/actuator/areacache             # everything, cluster-wide
curl -X DELETE "http://localhost:5401/actuator/areacache?type=PARISH" # one level and below
curl           http://localhost:5401/actuator/areacache             # per-region in-heap sizes
```

---

## Running

### Locally (from an IDE or `mvn spring-boot:run`)

Bring up just Postgres and Redis; run the app on the host:

```bash
docker compose -f docker-compose.dev.yml up -d          # postgres :5437, redis :8101
mvn spring-boot:run                                      # API :4401, management :5401
```

The `dev1` profile defaults already point at those ports — no configuration needed. Add
`--profile observability` to the compose command for Prometheus (:9090) and Grafana (:3000).

### Full stack in Docker

```bash
docker compose --profile app --profile observability up -d --build
```

Brings up Postgres, Redis, the API, the Angular console (nginx, same-origin), Prometheus and Grafana.
Credentials come from `.env` (see `.env.example`).

### Restoring a database dump

Compose brings up **infrastructure only** by default so you can restore before the API validates
against the schema:

```bash
docker compose up -d                                     # postgres + redis only
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ./backups/<dump>.sql
```

---

## Schema

Flyway owns the schema (`ddl-auto=validate` — the app refuses to start on drift). Migrations:

- `V1` baseline, `V2` soft-delete `archived` column, `V3` foreign-key and **functional** indexes
  (`upper(code)` / `upper(name)`, which the case-insensitive lookups actually use).

---

## Configuration

Everything is environment-overridable; the `dev1` defaults are for local use.

| Concern | Property / env | Default |
|---|---|---|
| API port | `APP-PORT` | 4401 |
| Management port | `MANAGEMENT-PORT` | 5401 (not published in Docker) |
| Postgres | `DB-URL` / `DB-USERNAME` / `DB-PASSWORD` | `localhost:5437/areadevdb` |
| Redis | `REDIS-HOST` / `REDIS-PORT` | `localhost:8101` |
| CORS origins | `CORS-ORIGINS` | `http://localhost:4200` |
| Rate limit | `RATE-LIMIT-CAPACITY` | 300 / min / client |
| Trust `X-Forwarded-For` | `RATE-LIMIT-TRUST-FORWARDED-FOR` | `false` |
| API docs | `API-DOCS-ENABLED` | `true` |

**CORS** is a browser mechanism and does not restrict server-to-server callers (they send no
`Origin`). **Rate limiting** is per-pod and keyed on the peer address by default; only trust
`X-Forwarded-For` when a proxy you control is the sole path in (see `RateLimitProperties`).

---

## Observability

On the management port (`:5401`): `/actuator/health` (with `liveness`/`readiness` probes),
`/actuator/prometheus`, `/actuator/metrics`, `/actuator/caches`, `/actuator/areacache`.

The Grafana dashboard (provisioned under the `observability` profile) shows cache hit ratio per tier
and level, the **L2 error rate** (the one to alert on — non-zero means Redis is failing and the
service is quietly running on Postgres), request latency percentiles, and the Hikari pool, which with
virtual threads is the real throughput ceiling.

---

## Tests

```bash
mvn test
```

A fast H2 suite covers the frozen response contract (byte-for-byte, including error `detail`
strings), the two-tier cache semantics, the eviction cascade, cross-pod invalidation, `?view=flat`,
ETags, CORS, and rate limiting.
