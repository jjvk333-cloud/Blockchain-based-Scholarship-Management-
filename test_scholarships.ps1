$utf8 = New-Object System.Text.UTF8Encoding($false)

Write-Host "=== 1. Log in as Admin ===" -ForegroundColor Cyan
$adminLogin = @{
    email = "admin@college.edu"
    password = "AdminPassword@123"
} | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminAuth.data.token
Write-Host "Admin Logged In! Token acquired." -ForegroundColor Green

Write-Host "`n=== 2. Admin Creates a Scholarship ===" -ForegroundColor Cyan
$schReq = @{
    title = "Merit-cum-Means National Scholarship 2026"
    description = "Annual grant for high-achieving STEM students with demonstrated financial need."
    minGpa = 8.00
    maxAnnualIncome = 300000.00
    grantAmount = 50000.00
    deadline = "2026-12-31 23:59:59"
} | ConvertTo-Json

$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
$createdSch = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships" -Method Post -Headers $adminHeaders -Body $schReq -ContentType "application/json"
$schId = $createdSch.data.id
Write-Host "Scholarship Created with ID: $schId" -ForegroundColor Green
Write-Host "Title: $($createdSch.data.title), Grant: ₹$($createdSch.data.grantAmount)" -ForegroundColor Yellow

Write-Host "`n=== 3. Public / Student Views Active Scholarships ===" -ForegroundColor Cyan
$publicList = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships" -Method Get
Write-Host "Found $($publicList.data.Count) active scholarship(s)." -ForegroundColor Green

Write-Host "`n=== 4. Student Logs In & Checks Eligibility ===" -ForegroundColor Cyan
$studLogin = @{
    email = "student@college.edu"
    password = "Password@123"
} | ConvertTo-Json
$studAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $studLogin -ContentType "application/json"
$studToken = $studAuth.data.token

$studHeaders = @{ "Authorization" = "Bearer $studToken" }
$eligibility = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships/$schId/check-eligibility" -Method Get -Headers $studHeaders
Write-Host "Eligibility for Aarav (GPA: 8.75, Income: 2.5L):" -ForegroundColor Cyan
Write-Host "Eligible: $($eligibility.data.eligible)" -ForegroundColor Green
Write-Host "Reasons: $($eligibility.data.eligibilityReasons -join '; ')" -ForegroundColor Yellow

Write-Host "`n=== 5. Non-Eligible Student Test ===" -ForegroundColor Cyan
$ineligibleStudent = @{
    email = "lowgpa.student@college.edu"
    password = "Password@123"
    fullName = "Rahul Gupta"
    role = "ROLE_STUDENT"
    rollNumber = "CS2026-099"
    department = "Civil Engineering"
    gpa = 6.80
    annualFamilyIncome = 450000.00
    walletAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"
    phone = "+919876500000"
} | ConvertTo-Json
try {
    $regIneligible = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $ineligibleStudent -ContentType "application/json"
    $ineligToken = $regIneligible.data.token
} catch {
    $ineligLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body (@{email="lowgpa.student@college.edu"; password="Password@123"} | ConvertTo-Json) -ContentType "application/json"
    $ineligToken = $ineligLogin.data.token
}

$ineligHeaders = @{ "Authorization" = "Bearer $ineligToken" }
$ineligCheck = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships/$schId/check-eligibility" -Method Get -Headers $ineligHeaders
Write-Host "Eligibility for Rahul (GPA: 6.80, Income: 4.5L):" -ForegroundColor Cyan
Write-Host "Eligible: $($ineligCheck.data.eligible)" -ForegroundColor Red
Write-Host "Reasons: $($ineligCheck.data.eligibilityReasons -join '; ')" -ForegroundColor Yellow