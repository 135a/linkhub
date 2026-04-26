$ErrorActionPreference = "Stop"

$frontendApiFile = "console-vue/src/api/axios.js"

if (!(Test-Path $frontendApiFile)) {
  throw "Contract check files missing."
}

$frontendContent = Get-Content -Path $frontendApiFile -Raw

if ($frontendContent -notmatch "/api/short-link/v1") {
  throw "Frontend API base path is not aligned with expected contract."
}

if ($frontendContent -match "/api/short-link/admin/v1") {
  throw "Frontend still points to legacy admin route."
}

Write-Host "API contract check passed." -ForegroundColor Green
