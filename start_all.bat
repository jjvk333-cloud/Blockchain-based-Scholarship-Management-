@echo off
setlocal enabledelayedexpansion
title ScholarTrust Complete Setup ^& Launcher
echo ============================================================
echo   ScholarTrust Blockchain Scholarship Platform
echo   One-Click Automated Setup ^& Launcher
echo ============================================================
echo.

cd /d "%~dp0"

:: 1. Auto-create .env file from template if missing
if not exist ".env" (
    echo [SETUP 1/4] .env file not found. Generating default development .env...
    (
        echo # ScholarTrust Local Environment Configuration
        echo SERVER_PORT=8080
        echo DB_URL=jdbc:mysql://localhost:3306/scholartrust_db?createDatabaseIfNotExist=true^&useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC
        echo DB_USERNAME=root
        echo DB_PASSWORD=venkat123
        echo UPLOAD_DIR=uploads/documents
        echo JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
        echo JWT_EXPIRATION=86400000
        echo BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545
        echo BLOCKCHAIN_CONTRACT_ADDRESS=0x5b1869D9A4C187F2EAa108f3062412ecf0526b24
        echo BLOCKCHAIN_ADMIN_PRIVATE_KEY=0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d
    ) > .env
    echo [SETUP 1/4] Created .env with standard local credentials.
) else (
    echo [SETUP 1/4] Existing .env file detected. Keeping existing configuration.
)

:: 2. Ensure node_modules dependencies are installed
if not exist "node_modules\" (
    echo.
    echo [SETUP 2/4] Installing Node.js blockchain packages (ganache, web3, solc)...
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed. Please check your internet connection and Node.js installation.
        pause
        exit /b %errorlevel%
    )
) else (
    echo [SETUP 2/4] Node.js dependencies already installed.
)

:: 3. Launch Ganache Node
echo.
echo [SETUP 3/4] Starting Ganache local blockchain node in background...
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

:: 4. Deploy smart contract and synchronize .env & deployment.json
echo.
echo [SETUP 4/4] Compiling and deploying Solidity contract to Ganache...
node contracts/deploy.js
if %errorlevel% neq 0 (
    echo [ERROR] Smart contract deployment failed!
    pause
    exit /b %errorlevel%
)

:: 5. Start Spring Boot Backend
echo.
echo ============================================================
echo [STARTING] Launching Spring Boot backend...
echo ============================================================
start "ScholarTrust Backend" cmd /k "cd /d ""%~dp0"" && mvn spring-boot:run"

echo.
echo ============================================================
echo   ScholarTrust is launching!
echo   Web Dashboard will be ready at:
echo   --^> http://localhost:8080
echo ============================================================
echo Press any key to close this launcher window (services keep running).
pause >nul