@echo off
title ScholarTrust System Launcher
echo ============================================================
echo Launching ScholarTrust Blockchain Scholarship Platform...
echo ============================================================
echo [1/3] Starting Ganache node in separate window...
start "Ganache Node" cmd /k "npx ganache --wallet.deterministic --server.port 8545"

echo Waiting 6 seconds for Ganache to initialize...
timeout /t 6 /nobreak >nul

echo [2/3] Deploying smart contract...
node contracts/deploy.js

echo [3/3] Starting Spring Boot backend in separate window...
start "ScholarTrust Backend" cmd /k "mvn spring-boot:run"

echo ============================================================
echo ScholarTrust is launching!
echo Once the backend finishes starting, visit:
echo http://localhost:8080
echo ============================================================
pause