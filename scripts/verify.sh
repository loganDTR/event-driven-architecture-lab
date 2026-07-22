#!/usr/bin/env bash
set -euo pipefail

echo "== Container status =="
docker compose ps

echo
echo "== Redpanda cluster =="
docker compose exec redpanda rpk cluster info

echo
echo "== Create topic =="
docker compose exec redpanda rpk topic create lab.events --partitions 3 --replicas 1 || true

echo
echo "== Topic list =="
docker compose exec redpanda rpk topic list

echo
echo "== PostgreSQL =="
docker compose exec postgres psql -U eventlab -d eventlab -c   "SELECT id, customer_id, status, total_amount, created_at FROM lab.orders ORDER BY id;"
