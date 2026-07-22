$ErrorActionPreference = "Stop"

Write-Host "== Container status =="
docker compose ps

Write-Host ""
Write-Host "== Redpanda cluster =="
docker compose exec redpanda rpk cluster info

Write-Host ""
Write-Host "== Create topic =="
docker compose exec redpanda rpk topic create lab.events --partitions 3 --replicas 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Topic already present or creation returned a non-zero status; continuing."
}

Write-Host ""
Write-Host "== Topic list =="
docker compose exec redpanda rpk topic list

Write-Host ""
Write-Host "== PostgreSQL =="
docker compose exec postgres psql -U eventlab -d eventlab -c "SELECT id, customer_id, status, total_amount, created_at FROM lab.orders ORDER BY id;"
