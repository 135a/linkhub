$ErrorActionPreference = "Stop"

function Assert-StatusCode {
  param(
    [string]$Url,
    [int]$Expected
  )
  $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
  if ($resp.StatusCode -ne $Expected) {
    throw "Request to $Url expected $Expected, got $($resp.StatusCode)"
  }
}

Assert-StatusCode -Url "http://localhost/healthz" -Expected 200
Assert-StatusCode -Url "http://localhost/" -Expected 200

try {
  $api = Invoke-WebRequest -Uri "http://localhost/api/short-link/v1/count?requestParam=default" -UseBasicParsing -TimeoutSec 10
  if ($api.StatusCode -lt 200 -or $api.StatusCode -ge 500) {
    throw "API smoke status unexpected: $($api.StatusCode)"
  }
} catch {
  throw "API smoke check failed: $($_.Exception.Message)"
}

Write-Host "Nginx smoke check passed." -ForegroundColor Green
