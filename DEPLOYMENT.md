# Deployment

How to build, tag, push and run the Administrative Area stack. The stack is two application images —
the API and the Angular console — plus Postgres, Redis, Prometheus and Grafana, wired together by
[`docker-compose.yml`](docker-compose.yml).

All commands run from this directory (`AdministrativeareaApi/`), which holds the compose file and the
API `Dockerfile`; the console's `Dockerfile` lives in `../AdministrativeArea-Frontend` and is built
from here through compose.

---

## 1. Prerequisites

- Docker with Compose v2 (`docker compose`, not `docker-compose`).
- A container registry you can push to, and `docker login <registry>` already done.
- A populated `.env` (copy `.env.example`). For a real deployment, review every value in
  [§5 Production configuration](#5-production-configuration) — the defaults are for local use.

Set the registry and version once; the rest of this guide uses them:

```bash
export REGISTRY=registry.example.com/administrative-area
export VERSION=$(date +%Y-%m-%d)          # e.g. 2026-07-15 — or a git tag / release number
```

Using a **date or release tag, never `latest`**, is deliberate: `latest` makes "what is actually
running" unknowable and makes rollback guesswork. Every deploy should reference an immutable tag.

---

## 2. Build the images

The API `Dockerfile` builds the jar itself (Maven build stage) and the console `Dockerfile` builds
the Angular bundle, so no local `mvn` or `npm` is required — only Docker.

```bash
docker compose --profile app build
```

This produces `administrative-area-api:local` and `administrative-area-ui:local`.

> Add `--no-cache` for a clean release build (re-resolves Maven and npm dependencies from scratch),
> at the cost of several minutes. Use it when cutting a versioned release; skip it for quick iteration.

---

## 3. Tag for the registry

Tag each built image with both the immutable version and the registry path. Tagging is free and does
not copy data — every tag points at the same image digest.

```bash
docker tag administrative-area-api:local ${REGISTRY}-api:${VERSION}
docker tag administrative-area-ui:local  ${REGISTRY}-ui:${VERSION}

# Optional convenience tag; the versioned tag above is the source of truth.
docker tag administrative-area-api:local ${REGISTRY}-api:latest
docker tag administrative-area-ui:local  ${REGISTRY}-ui:latest
```

---

## 4. Push to the registry

```bash
docker push ${REGISTRY}-api:${VERSION}
docker push ${REGISTRY}-ui:${VERSION}
docker push ${REGISTRY}-api:latest        # only if you created it
docker push ${REGISTRY}-ui:latest
```

Record the pushed digests (`docker inspect --format '{{index .RepoDigests 0}}' ${REGISTRY}-api:${VERSION}`)
if your target pins by digest.

---

## 5. Production configuration

The compose defaults target a dev box. Before deploying, set these in the target's `.env`:

| Variable | Dev default | Production |
|---|---|---|
| `APP_PROFILE` | `dev1` | your prod profile, or override the values below |
| `DDL_AUTO` | `validate` | keep `validate` — Flyway owns the schema; never `update` |
| `POSTGRES_PASSWORD` | placeholder | a real secret, from your secret store |
| `CORS_ORIGINS` | `` (same-origin) | only browser origins served from another host; empty if the console is same-origin behind nginx |
| `RATE_LIMIT_CAPACITY` | `300` | tune per client, per pod |
| `RATE_LIMIT_TRUST_FORWARDED_FOR` | `false` | `true` **only** if the API port is unpublished and a proxy you control is the sole path in |
| `API_DOCS_ENABLED` | `true` | `false` to withdraw Swagger UI and `/v3/api-docs` |
| `REDIS_MAXMEMORY` | `1gb` | size to the working set; policy is `allkeys-lru` |

Two things that are configuration by omission, not accident:

- **The management port (9084) is never published.** Actuator — health, metrics, heap, the cache
  eviction endpoint — is reachable inside the compose network (Prometheus, the healthcheck) but not
  from outside. Do not add it to `ports:`.
- **The API port (8084) is published** so other services can call it. That is why
  `RATE_LIMIT_TRUST_FORWARDED_FOR` is `false`: a caller that reaches the API directly could otherwise
  forge `X-Forwarded-For` and evade the limiter. Only flip it to `true` in a topology where the API
  port is *not* exposed and nginx is the only ingress.

---

## 6. Deploy on the target

On the target host, with the same `docker-compose.yml`, `.env`, and the `docker/` provisioning
directory present, point compose at the pushed images and start the stack:

```bash
export API_IMAGE=${REGISTRY}-api:${VERSION}
export UI_IMAGE=${REGISTRY}-ui:${VERSION}

docker compose pull                                          # api, ui from the registry
docker compose --profile app --profile observability up -d
```

`API_IMAGE` / `UI_IMAGE` override the compose image refs (which otherwise default to the locally-built
`:local`). Pinning them to the versioned tag is what makes the running stack traceable to a build.

### First deploy: restore data first

Flyway runs `validate` and refuses to start against a schema it did not create. On a fresh database,
bring up infrastructure only and restore before the API starts:

```bash
docker compose up -d postgres redis
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ./backups/<dump>.sql
docker compose --profile app up -d          # now the API validates against real data
```

---

## 7. Verify the rollout

```bash
docker compose ps                                           # all services healthy

# API readiness (management port, from inside the network)
docker compose exec api wget -qO- http://localhost:9084/actuator/health/readiness

# The console, served same-origin with the API proxied under /api/
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:4200/
curl -s -o /dev/null -w '%{http_code}\n' "http://localhost:4200/api/v1/administrative-areas/search?type=REGION"
```

Then confirm in Grafana (`:3000`) that the cache hit ratio climbs and the **L2 error rate stays
zero** — a non-zero L2 error rate means Redis is failing and the service is silently running on
Postgres alone.

---

## 8. Roll back

Because every release is an immutable tag, rollback is re-pointing the image vars at the previous
version and restarting — no rebuild:

```bash
export API_IMAGE=${REGISTRY}-api:<previous-version>
export UI_IMAGE=${REGISTRY}-ui:<previous-version>
docker compose --profile app up -d

# If the bad release added a migration, roll the schema back the same deliberate way it went forward —
# Flyway does not auto-undo. Restore from a pre-deploy backup or apply a corrective migration.
```

Data in Postgres and Redis lives in named volumes and survives an image swap; only redeploy the
application images, never `docker compose down -v` (which deletes the volumes).

---

## Quick reference

```bash
# build → tag → push a release
docker compose --profile app build --no-cache
docker tag administrative-area-api:local ${REGISTRY}-api:${VERSION}
docker tag administrative-area-ui:local  ${REGISTRY}-ui:${VERSION}
docker push ${REGISTRY}-api:${VERSION}
docker push ${REGISTRY}-ui:${VERSION}

# run that release
API_IMAGE=${REGISTRY}-api:${VERSION} UI_IMAGE=${REGISTRY}-ui:${VERSION} \
  docker compose --profile app --profile observability up -d
```
