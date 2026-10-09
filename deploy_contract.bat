@echo off
title Deploy Smart Contract
cd /d "%~dp0"
echo ============================================================
echo Compiling and Deploying ScholarshipLedger.sol to Ganache...
echo ============================================================
node contracts/deploy.js
pause