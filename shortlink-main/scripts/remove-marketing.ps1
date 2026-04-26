# Script to remove all marketing/promotional content from the codebase
# Targets: NageOffer copyright blocks, WeChat marketing lines, nageoffer.com URLs

$rootDir = "d:\shortlink-main\shortlink-main"

# ============================================================
# 1. Remove entire copyright header block from Java files
#    Pattern: /* * Copyright ... NageOffer ... */
# ============================================================
Write-Host "=== Step 1: Removing NageOffer copyright headers ===" -ForegroundColor Cyan

$javaFiles = Get-ChildItem -Path $rootDir -Recurse -Include "*.java" -File
$copyrightPattern = '(?s)/\*\s*\r?\n\s*\*\s*Copyright\s*©.*?NageOffer.*?\*/\r?\n'

foreach ($file in $javaFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    if ($content -match "Copyright.*NageOffer") {
        $newContent = [regex]::Replace($content, $copyrightPattern, '')
        [System.IO.File]::WriteAllText($file.FullName, $newContent)
        Write-Host "  Removed copyright from: $($file.FullName)" -ForegroundColor Green
    }
}

# ============================================================
# 2. Remove marketing line from Javadoc comments
#    Pattern: * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
# ============================================================
Write-Host "`n=== Step 2: Removing WeChat marketing lines ===" -ForegroundColor Cyan

$marketingPattern = '\s*\*\s*公众号：马丁玩编程.*获取项目资料\r?\n'

foreach ($file in $javaFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    if ($content -match "马丁玩编程") {
        $newContent = [regex]::Replace($content, $marketingPattern, "`n")
        [System.IO.File]::WriteAllText($file.FullName, $newContent)
        Write-Host "  Removed marketing from: $($file.FullName)" -ForegroundColor Green
    }
}

# ============================================================
# 3. Remove nageoffer.com URL comments (entire comment lines)
#    Pattern: // ...详情查看：https://nageoffer.com/shortlink/question
# ============================================================
Write-Host "`n=== Step 3: Removing nageoffer.com URL comments ===" -ForegroundColor Cyan

$urlPattern = '\s*//.*nageoffer\.com.*\r?\n'

foreach ($file in $javaFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    if ($content -match "nageoffer\.com") {
        $newContent = [regex]::Replace($content, $urlPattern, "`n")
        [System.IO.File]::WriteAllText($file.FullName, $newContent)
        Write-Host "  Removed URL comments from: $($file.FullName)" -ForegroundColor Green
    }
}

# ============================================================
# 4. Remove MurmurHash nageoffer.com reference in demo.java
# ============================================================
Write-Host "`n=== Step 4: Cleaning demo.java ===" -ForegroundColor Cyan

$demoFile = Join-Path $rootDir "project\src\test\java\demo.java"
if (Test-Path $demoFile) {
    $content = [System.IO.File]::ReadAllText($demoFile)
    $content = $content -replace 'https://nageoffer\.com/shortlink', 'https://example.com/shortlink'
    [System.IO.File]::WriteAllText($demoFile, $content)
    Write-Host "  Cleaned demo.java" -ForegroundColor Green
}

# ============================================================
# 5. Fix frontend store - replace nageoffer domain
# ============================================================
Write-Host "`n=== Step 5: Fixing frontend store domain ===" -ForegroundColor Cyan

$storeFile = Join-Path $rootDir "console-vue\src\store\index.js"
if (Test-Path $storeFile) {
    $content = [System.IO.File]::ReadAllText($storeFile)
    $content = $content -replace "s\.nageoffer\.com", "link.example.com"
    [System.IO.File]::WriteAllText($storeFile, $content)
    Write-Host "  Fixed store domain" -ForegroundColor Green
}

# ============================================================
# 6. Update frontend index.html title
# ============================================================
Write-Host "`n=== Step 6: Updating frontend title ===" -ForegroundColor Cyan

$indexFile = Join-Path $rootDir "console-vue\index.html"
if (Test-Path $indexFile) {
    $content = [System.IO.File]::ReadAllText($indexFile)
    $content = $content -replace "<title>短链接</title>", "<title>Linkhub-Saas高性能短链接</title>"
    [System.IO.File]::WriteAllText($indexFile, $content)
    Write-Host "  Updated index.html title" -ForegroundColor Green
}

# ============================================================
# 7. Update frontend branding text
# ============================================================
Write-Host "`n=== Step 7: Updating frontend branding ===" -ForegroundColor Cyan

$homeFile = Join-Path $rootDir "console-vue\src\views\home\HomeIndex.vue"
if (Test-Path $homeFile) {
    $content = [System.IO.File]::ReadAllText($homeFile)
    $content = $content -replace "短链通系统", "Linkhub-Saas高性能短链接"
    [System.IO.File]::WriteAllText($homeFile, $content)
    Write-Host "  Updated HomeIndex.vue branding" -ForegroundColor Green
}

$loginFile = Join-Path $rootDir "console-vue\src\views\login\LoginIndex.vue"
if (Test-Path $loginFile) {
    $content = [System.IO.File]::ReadAllText($loginFile)
    $content = $content -replace "短链接管理平台", "Linkhub-Saas高性能短链接"
    [System.IO.File]::WriteAllText($loginFile, $content)
    Write-Host "  Updated LoginIndex.vue branding" -ForegroundColor Green
}

Write-Host "`n=== Done! All marketing content removed ===" -ForegroundColor Cyan
