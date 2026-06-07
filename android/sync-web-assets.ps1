# 将 Web 构建产物复制到 Android assets（与 Web 端 UI 同步）
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$webDir = Join-Path $root "web"
$dist = Join-Path $webDir "dist"
$target = Join-Path $PSScriptRoot "app\src\main\assets\www"

Write-Host ">> 构建 Web..."
Push-Location $webDir
npm run build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Pop-Location

if (-not (Test-Path $dist)) {
    Write-Error "未找到 $dist"
}

Write-Host ">> 复制到 $target"
if (Test-Path $target) { Remove-Item $target -Recurse -Force }
New-Item -ItemType Directory -Path $target -Force | Out-Null
Copy-Item -Path (Join-Path $dist "*") -Destination $target -Recurse -Force
Write-Host "Done. Re-run the app in Android Studio."
