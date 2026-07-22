# 01 — Event-Driven Lab Infrastructure

Local development infrastructure composed of:

- Redpanda: Kafka-compatible broker
- Redpanda Console: browser UI
- PostgreSQL 18
- Persistent named volumes
- Health checks
- Separate internal and host listeners

## Requirements

- Docker Desktop with Docker Compose v2
- At least 4 GB of free memory recommended

## Start

Copy the environment template:

### PowerShell

```powershell
Copy-Item .env.example .env
docker compose up -d
docker compose ps
```

Or:

```powershell
./scripts/start.ps1
```

### Bash / WSL

```bash
cp .env.example .env
chmod +x scripts/*.sh
./scripts/start.sh
```

## Endpoints

| Component | Host endpoint | Docker-network endpoint |
|---|---|---|
| Redpanda Console | http://localhost:8080 | `console:8080` |
| Kafka API | `localhost:19092` | `redpanda:9092` |
| Schema Registry | http://localhost:18081 | `http://redpanda:8081` |
| HTTP Proxy | http://localhost:18082 | `http://redpanda:8082` |
| Admin API | http://localhost:19644 | `http://redpanda:9644` |
| PostgreSQL | `localhost:5432` | `postgres:5432` |

PostgreSQL defaults are read from `.env`.

## Verify

### PowerShell

```powershell
./scripts/verify.ps1
```

### Bash / WSL

```bash
./scripts/verify.sh
```

The verification creates `lab.events` with three partitions and queries the
sample `lab.orders` table.

## Manual Kafka smoke test

Create a topic:

```powershell
docker compose exec redpanda rpk topic create lab.manual --partitions 3 --replicas 1
```

Start a consumer in terminal A:

```powershell
docker compose exec redpanda rpk topic consume lab.manual --offset start
```

Produce messages in terminal B:

```powershell
docker compose exec redpanda rpk topic produce lab.manual
```

Enter one JSON object per line, then press `Ctrl+Z` and Enter on Windows, or
`Ctrl+D` on Linux/WSL:

```json
{"eventId":"evt-001","type":"OrderCreated","orderId":1}
```

## PostgreSQL connection

```text
Host: localhost
Port: 5432
Database: eventlab
Username: eventlab
Password: eventlab_dev_password
```

From another Compose service, use host `postgres`, not `localhost`.

## Stop and reset

Stop containers while retaining data:

```powershell
docker compose down
```

Delete containers and all lab data:

```powershell
docker compose down -v
```

Initialization scripts run only when the PostgreSQL data volume is empty.
After changing `postgres/init/001-init.sql`, reset with `docker compose down -v`.

## Design notes

This first module intentionally uses one Redpanda broker. Replication factor 1
is correct for this topology; a three-broker cluster will be introduced later
when testing partition leadership, broker failure and consumer rebalancing.

Do not use these development credentials or this unsecured configuration in
production.
