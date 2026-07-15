# Administrative Area — build images + run the production compose

`AdministrativeareaApi/docker-compose.prod.yml` is a **single self-contained file** (all values
inlined, no `.env`). It pulls pre-built images from Docker Hub (`wanfadger/administrative-area-*`) and
runs one Postgres, one Redis, the API and the console. The deployment team only needs the compose
file.

> **Repo location:** this file lives at `AdministrativeareaApi/docker-compose.prod.yml`
> (version-controlled alongside the dev `AdministrativeareaApi/docker-compose.yml`). Commands below run
> from `AdministrativeareaApi/`; on a standalone deploy host you can copy the single file anywhere.

## 0. One-time: create the DB dump from dev

The restore in step 3b needs one SQL dump. Dev runs a single Postgres container (`aa-postgres`) that
holds the `areadevdb` database; export it.

```bash
# make sure the dev DB is up first
docker compose -f docker-compose.dev.yml up -d postgres

docker exec aa-postgres pg_dump -U siip-db-user-dev -d areadevdb > areadb.sql
```

## 1. Build images (from `AdministrativeareaApi/`)

Each image builds from its own Dockerfile — the API builds its jar (Maven stage) and the console
builds the Angular bundle, so only Docker is needed, no local `mvn`/`npm`. The console's API URL is
**not** baked in (it is runtime config, defaulting to same-origin), so there is no build-arg to set.

```bash
NS=wanfadger; TAG=2026-07-15   # dated build tag; the compose pins it via ${IMAGE_TAG} (or use TAG=latest)

docker build -t "$NS/administrative-area-api:$TAG" .
docker build -t "$NS/administrative-area-ui:$TAG" ../AdministrativeArea-Frontend
```

Add `--no-cache` for a clean release build. Check them with
`docker images | grep administrative-area` (expect 2 images).

## 2. Push images

```bash
docker login
NS=wanfadger; TAG=2026-07-15   # reuse the same dated tag you built in step 1

docker push "$NS/administrative-area-api:$TAG"
docker push "$NS/administrative-area-ui:$TAG"
```

## 3. Deploy (on the live host — needs only `docker-compose.prod.yml`)

Two-phase, mirroring development — DB up + restored first, then the API runs its Flyway migrations on
top. Flyway is set to `validate`, so it refuses to start against a schema it did not create; the
restore has to come first.

> **Stop the dev stack first** if it is on the same host — it reuses the Postgres/Redis host ports.
> `docker compose down` (from `AdministrativeareaApi/`).

```bash
# a) database + redis
docker compose -f docker-compose.prod.yml up -d postgres redis

# b) restore the DB (the team's existing way). Plain SQL example:
docker exec -i aa-postgres psql -U siip-db-user-dev -d areadevdb < areadb.sql

# c) the rest — pin the image tag you pushed. The API waits for postgres/redis to report healthy
#    (depends_on conditions) before starting, then runs Flyway migrations.
IMAGE_TAG=2026-07-15 docker compose -f docker-compose.prod.yml up -d
```

> Testing the **hosted** images on the same machine you built on? `up` reuses the local copies and
> won't hit Docker Hub. To prove the pushed images actually pull, force it:
> `IMAGE_TAG=2026-07-15 docker compose -f docker-compose.prod.yml pull` before `up` (or `docker image rm`
> the local `wanfadger/administrative-area-*` first).

Verify:

```bash
docker compose -f docker-compose.prod.yml ps                         # all healthy
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:4200/      # console
curl -s -o /dev/null -w '%{http_code}\n' \
  "http://localhost:4200/api/v1/administrative-areas/search?type=REGION"   # API, same-origin via nginx
```

## 4. Ports

| Host | What |
|------|------|
| 4200 | Console (Angular UI, nginx — also proxies `/api/` to the API) |
| 8084 | API (single entrypoint for integrating services) |
| 5437 | Postgres |

The API's management port (9084) is **internal-only** — health, metrics and the cache-eviction
endpoint are reachable inside the docker network, not from outside. Redis has no published port.

## 5. Adjust for the live host

Edit these directly in `docker-compose.prod.yml` if they differ:

- **Image tag** — set once via `IMAGE_TAG` at deploy (`IMAGE_TAG=2026-07-15 docker compose … up -d`);
  defaults to `latest`. No need to edit each image line.
- **DB password** — set `POSTGRES_PASSWORD` and the API's `DB_PASSWORD` to the same strong value
  (they ship as `__SET_A_STRONG_PASSWORD__` placeholders). This is the password the role is created
  with on first start; it does not need to match the source dump.
- **`CORS_ORIGINS`** — leave empty for the same-origin console; add an origin only for a browser app
  served from another host.
- **`RATE_LIMIT_TRUST_FORWARDED_FOR`** — keep `false` while port 8084 is published. Set `true` only
  after removing the API's published port so nginx is the sole ingress; otherwise a caller can forge
  `X-Forwarded-For` and evade the limiter.
- **`API_DOCS_ENABLED`** — set `false` to withdraw Swagger UI and `/v3/api-docs` in production.

> In the compose: per-container healthchecks, health-gated startup (`depends_on: service_healthy`),
> resource limits, container-aware JVM (`MaxRAMPercentage` + ZGC), and a two-tier cache that keeps
> serving from Postgres if Redis goes down. Data lives in named volumes (`pgdata`, `redisdata`) — never
> `docker compose down -v`, which deletes them. Still deferred: auto-restore and secrets management.
