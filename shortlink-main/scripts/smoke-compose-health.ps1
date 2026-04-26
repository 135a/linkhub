$ErrorActionPreference = "Stop"

Write-Host "Starting compose stack..." -ForegroundColor Cyan
docker compose up -d

$servicesToCheck = @(
  "project",
  "frontend",
  "rocketmq-namesrv",
  "rocketmq-broker"
)

foreach ($service in $servicesToCheck) {
  $status = docker compose ps --format json $service | ConvertFrom-Json
  if (-not $status) {
    throw "Service '$service' not found in docker compose output."
  }
  if ($status.State -ne "running") {
    throw "Service '$service' is not running. State=$($status.State)"
  }
  if ($status.Health -and $status.Health -ne "healthy") {
    throw "Service '$service' health is '$($status.Health)'."
  }
}

Write-Host "Compose health smoke check passed." -ForegroundColor Green
