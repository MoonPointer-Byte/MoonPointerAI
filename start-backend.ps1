# AI 同声传译后端启动脚本（DeepSeek）
# 配置请编辑 backend/application-local.yml

Set-Location $PSScriptRoot\backend

if (-not (Test-Path "application-local.yml")) {
    Write-Host "未找到 application-local.yml，正在从模板创建..." -ForegroundColor Yellow
    Copy-Item "application-local.yml.example" "application-local.yml"
    Write-Host "请编辑 backend/application-local.yml 填入 DeepSeek API Key，然后重新运行此脚本。" -ForegroundColor Yellow
    Write-Host "申请 Key: https://platform.deepseek.com/api_keys" -ForegroundColor Gray
    exit 1
}

# 检查 8080 端口是否已有后端在运行
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -TimeoutSec 3
    $notesOk = $false
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/api/notes/current?clientSessionId=ping" -TimeoutSec 3 | Out-Null
        $notesOk = $true
    } catch { }
    $aiNotesOk = $health.notesPipeline -eq "langchain4j"
    if ($health.status -eq "ok" -and $notesOk -and $aiNotesOk) {
        Write-Host "后端已在运行中 (llmConfigured=$($health.llmConfigured), dbReady=$($health.dbReady))，无需重复启动。" -ForegroundColor Green
        Write-Host "访问 http://localhost:8080/api/health 可验证状态。" -ForegroundColor Gray
        exit 0
    }
    if ($health.status -eq "ok" -and (-not $notesOk -or -not $aiNotesOk)) {
        Write-Host "检测到旧版后端（缺少笔记/AI 笔记 API），正在重启..." -ForegroundColor Yellow
        $portUsed = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        if ($portUsed) { Stop-Process -Id $portUsed.OwningProcess -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 2
    }
} catch {
    # 端口被占用但非本服务，或健康检查失败，继续尝试启动
    $portUsed = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($portUsed) {
        Write-Host "警告: 8080 端口已被占用 (PID $($portUsed.OwningProcess))，请先关闭占用进程再启动。" -ForegroundColor Red
        Write-Host "可用命令: Stop-Process -Id $($portUsed.OwningProcess) -Force" -ForegroundColor Gray
        exit 1
    }
}

Write-Host "启动后端服务（读取 application-local.yml）..." -ForegroundColor Cyan
mvn spring-boot:run
