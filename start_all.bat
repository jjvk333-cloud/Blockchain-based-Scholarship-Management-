@echo off
setlocal enabledelayedexpansion
title ScholarTrust System Launcher
echo ============================================================
echo   ScholarTrust Blockchain Scholarship Platform Launcher
echo ============================================================

cd /d "%~dp0"

echo [1/3] Checking & Starting Ganache node...
start "Ganache Node" cmd /k "cd /d ""%~dp0"" && npx ganache --wallet.deterministic --server.port 8545 --chain.chainId 1337"

echo Polling Ganache RPC on http://127.0.0.1:8545 until responsive...
:poll_ganache
timeout /t 1 /nobreak >nul
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8545' -Method POST -Body '{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}' -ContentType 'application/json' -TimeoutSec 1; exit 0 } catch { exit 1 }"
if %errorlevel% neq 0 (
    echo   Waiting for Ganache RPC...
    goto poll_ganache
)
echo Ganache RPC is ONLINE!

echo.
echo [2/3] Deploying smart contract and auto-syncing properties...
node contracts/deploy.js
if %errorlevel% neq 0 (
    echo Smart contract deployment failed!
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] Starting Spring Boot backend in separate window...
start "ScholarTrust Backend" cmd /k "cd /d ""%~dp0"" && mvn spring-boot:run"

echo ============================================================
echo ScholarTrust is launching!
echo Once Spring Boot finishes compiling and starts, visit:
echo http://localhost:8080
echo ============================================================
pause