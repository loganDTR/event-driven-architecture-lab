$ErrorActionPreference = "Stop"

docker compose up -d
docker compose ps

Write-Host ""
Write-Host "Redpanda Console: http://localhost:8080"
Write-Host "Kafka bootstrap:  localhost:19092"
Write-Host "PostgreSQL:       localhost:5432"
