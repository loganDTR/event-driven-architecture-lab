#!/usr/bin/env bash
set -euo pipefail

docker compose up -d
docker compose ps

echo
echo "Redpanda Console: http://localhost:8080"
echo "Kafka bootstrap:  localhost:19092"
echo "PostgreSQL:       localhost:5432"
