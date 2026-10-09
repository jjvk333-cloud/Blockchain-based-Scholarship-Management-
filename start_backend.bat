@echo off
title ScholarTrust Backend (Port 8080)
cd /d "%~dp0"
echo ============================================================
echo Starting ScholarTrust Spring Boot Application...
echo Access in browser: http://localhost:8080
echo ============================================================
mvn spring-boot:run
pause