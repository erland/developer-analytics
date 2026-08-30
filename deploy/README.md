# Reference Docker Compose deployment

The reference deployment runs the current version 1 baseline as three services:

- `web` — the built React application served by Nginx and the only service exposed to the host;
- `backend` — the Quarkus API, reachable from Nginx on the internal `web` network;
- `db` — PostgreSQL with persistent storage on an internal-only `data` network.

The background `worker` service is introduced later in the development plan.

## Start the stack

From the repository root:

```bash
cp deploy/env.example deploy/.env
# Edit deploy/.env and replace DB_PASSWORD.
docker compose --env-file deploy/.env -f deploy/compose.yaml up --build
```

Then open:

```text
http://localhost:8080/
```

The backend is intentionally not published on a host port. API traffic is routed through Nginx under `/api/`.

## Stop the stack

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml down
```

To also remove the PostgreSQL volume and all local database data:

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml down -v
```

## Network model

```text
host -> web/nginx -> backend -> PostgreSQL
                    |          ^
                    +----------+
```

`db` is attached only to the internal `data` network. `backend` bridges the `web` and `data` networks. `web` has no direct database access.
