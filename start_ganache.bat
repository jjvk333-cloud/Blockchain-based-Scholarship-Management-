@echo off
title Ganache Local Blockchain (Port 8545)
cd /d "%~dp0"
echo ============================================================
echo Starting Ganache Deterministic Node on http://127.0.0.1:8545
echo ============================================================
npx ganache --wallet.deterministic --server.port 8545 --chain.chainId 1337
pause