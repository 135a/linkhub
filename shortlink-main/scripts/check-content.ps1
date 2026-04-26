param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"

$bannedWords = @("拿个Offer", "马丁", "马哥", "马丁玩编程", "nageoffer.com")
$paths = @("console-vue/src/views", "console-vue/src/layout", "README.md", "docs")
$files = @()

foreach ($p in $paths) {
  $target = Join-Path $Root $p
  if (Test-Path $target) {
    if ((Get-Item $target).PSIsContainer) {
      $files += Get-ChildItem -Path $target -Recurse -File -Include *.md,*.vue,*.js,*.ts,*.java,*.xml,*.yaml,*.yml
    } else {
      $files += Get-Item $target
    }
  }
}

$hit = $false
foreach ($file in $files) {
  $content = Get-Content -Path $file.FullName -Raw
  foreach ($word in $bannedWords) {
    if ($content -match [regex]::Escape($word)) {
      Write-Host "Found banned word '$word' in $($file.FullName)" -ForegroundColor Red
      $hit = $true
    }
  }
}

if ($hit) {
  throw "Content check failed."
}

Write-Host "Content check passed." -ForegroundColor Green
