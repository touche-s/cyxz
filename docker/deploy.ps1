# ============================================================
# Cyxz Docker One-Click Deploy Script
# Usage: cd docker; .\deploy.ps1
# ============================================================
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Cyxz Docker Deploy" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Check .env
if (-not (Test-Path ".env")) {
    Write-Host "[1/5] .env not found, copying from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "  Please edit .env and re-run!" -ForegroundColor Red
    exit 1
}
Write-Host "[1/5] .env OK" -ForegroundColor Green

# 2. Check Docker
Write-Host "[2/5] Checking Docker..." -ForegroundColor Yellow
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Docker is not running! Start Docker Desktop first." -ForegroundColor Red
    exit 1
}
Write-Host "  Docker is running" -ForegroundColor Green

# 3. Build Maven deps image (pom 没变时层缓存命中，秒级完成)
Write-Host "[3/5] Building Maven deps image..." -ForegroundColor Yellow
docker build -f maven-deps.Dockerfile -t cyxz-maven-deps:latest ..
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Maven deps image build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "  Maven deps image ready" -ForegroundColor Green

# 4. Build and start
Write-Host "[4/5] Building images and starting containers..." -ForegroundColor Yellow
docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "  Build complete" -ForegroundColor Green

# 5. Wait and check status
Write-Host "[5/5] Waiting for containers to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host ""
Write-Host "Container Status:" -ForegroundColor Cyan
docker compose ps --format "table {{.Name}}`t{{.Status}}`t{{.Ports}}"

# minio-init 是 one-shot 容器，正常退出不算失败
$running = (docker compose ps --status running -q 2>$null).Count
$exited = (docker compose ps --status exited -q 2>$null).Count
$total = $running + $exited
Write-Host ""
if ($total -eq 0) {
    Write-Host "No containers found. Check: docker compose logs" -ForegroundColor Red
} elseif ($running -ge 17) {
    Write-Host "$running containers running" -ForegroundColor Green
    if ($exited -gt 0) {
        Write-Host "  ($exited one-shot init container(s) completed)" -ForegroundColor DarkGray
    }
} else {
    $failed = 17 - $running
    Write-Host "$running/17 running, $failed failed" -ForegroundColor Red
    Write-Host "Check logs: docker compose logs <service>" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "URLs:" -ForegroundColor Cyan
Write-Host "  Frontend:   http://localhost:80" -ForegroundColor White
Write-Host "  Gateway:    http://localhost:8080" -ForegroundColor White
Write-Host "  Nacos:      http://localhost:8848/nacos" -ForegroundColor White
Write-Host "  RabbitMQ:   http://localhost:15672  (guest/guest)" -ForegroundColor White
Write-Host "  MinIO:      http://localhost:9001  (see .env for credentials)" -ForegroundColor White
Write-Host ""
