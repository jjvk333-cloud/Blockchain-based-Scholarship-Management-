Write-Host "=== 1. Testing Student Registration ===" -ForegroundColor Cyan
$studentBody = @{
    email = "student@college.edu"
    password = "Password@123"
    fullName = "Aarav Sharma"
    role = "ROLE_STUDENT"
    rollNumber = "CS2026-001"
    department = "Computer Science"
    gpa = 8.75
    annualFamilyIncome = 250000.00
    walletAddress = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
    phone = "+919876543210"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $studentBody -ContentType "application/json"
    Write-Host "Student Registered Successfully!" -ForegroundColor Green
    $token = $regResponse.data.token
    Write-Host "JWT Token received: $($token.Substring(0, 25))..." -ForegroundColor Yellow
} catch {
    Write-Host "Registration error: $_" -ForegroundColor Red
}

Write-Host "`n=== 2. Testing Student Login ===" -ForegroundColor Cyan
$loginBody = @{
    email = "student@college.edu"
    password = "Password@123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login Successful!" -ForegroundColor Green
    $token = $loginResponse.data.token
} catch {
    Write-Host "Login error: $_" -ForegroundColor Red
}

Write-Host "`n=== 3. Testing Protected /api/auth/me with Bearer Token ===" -ForegroundColor Cyan
try {
    $headers = @{ "Authorization" = "Bearer $token" }
    $meResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -Headers $headers
    Write-Host "Access Granted to Protected Endpoint!" -ForegroundColor Green
    $meResponse.data | Format-List
} catch {
    Write-Host "Protected access error: $_" -ForegroundColor Red
}