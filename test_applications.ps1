$utf8 = New-Object System.Text.UTF8Encoding($false)

Write-Host "=== 1. Preparing Sample Student Documents ===" -ForegroundColor Cyan
"B.Tech Computer Science 8th Sem Marksheet - GPA: 8.75 - Verified by University" | Set-Content "sample_marksheet.txt" -Encoding UTF8
"Annual Family Income Certificate - Gross Income: Rs 2,50,000 - Tehsildar Office" | Set-Content "sample_income.txt" -Encoding UTF8
"National Identity Proof - Aadhaar / Student Card ID" | Set-Content "sample_id.txt" -Encoding UTF8

Write-Host "`n=== 2. Student Login ===" -ForegroundColor Cyan
$studLogin = @{
    email = "student@college.edu"
    password = "Password@123"
} | ConvertTo-Json
$studAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $studLogin -ContentType "application/json"
$studToken = $studAuth.data.token
Write-Host "Student Logged In! Token acquired." -ForegroundColor Green

# Get active scholarship
$schList = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships" -Method Get
$schId = $schList.data[0].id
Write-Host "Applying for Scholarship ID: $schId ($($schList.data[0].title))" -ForegroundColor Yellow

Write-Host "`n=== 3. Submitting Multipart Application with Documents ===" -ForegroundColor Cyan
# Use curl.exe for multipart upload
$curlOutput = & curl.exe -s -X POST "http://localhost:8080/api/applications/apply" `
  -H "Authorization: Bearer $studToken" `
  -F "scholarshipId=$schId" `
  -F "marksheet=@sample_marksheet.txt;type=text/plain" `
  -F "incomeCertificate=@sample_income.txt;type=text/plain" `
  -F "idProof=@sample_id.txt;type=text/plain"

$appJson = $curlOutput | ConvertFrom-Json
if ($appJson.success) {
    Write-Host "Application Submitted Successfully! ID: $($appJson.data.id)" -ForegroundColor Green
    Write-Host "Status: $($appJson.data.status)" -ForegroundColor Yellow
    foreach ($doc in $appJson.data.documents) {
        Write-Host " - $($doc.documentType): SHA-256 = $($doc.sha256Hash)" -ForegroundColor Cyan
    }
} else {
    Write-Host "Application failed: $($appJson.message)" -ForegroundColor Red
}

$appId = $appJson.data.id
$docId = $appJson.data.documents[0].id

Write-Host "`n=== 4. Admin Logs In & Verifies Document Integrity ===" -ForegroundColor Cyan
$adminLogin = @{
    email = "admin@college.edu"
    password = "AdminPassword@123"
} | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminAuth.data.token

$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
$verifyResult = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/applications/documents/$docId/verify-hash" -Method Post -Headers $adminHeaders
Write-Host "Verification Status: $($verifyResult.data.status)" -ForegroundColor Green
Write-Host "Calculated Hash: $($verifyResult.data.calculatedHash)" -ForegroundColor Cyan
Write-Host "Database Hash:   $($verifyResult.data.recordedDatabaseHash)" -ForegroundColor Cyan
Write-Host "Verdict: $($verifyResult.data.verdictDetails)" -ForegroundColor Yellow

Write-Host "`n=== 5. Duplicate Application Prevention Test ===" -ForegroundColor Cyan
$dupOutput = & curl.exe -s -X POST "http://localhost:8080/api/applications/apply" `
  -H "Authorization: Bearer $studToken" `
  -F "scholarshipId=$schId" `
  -F "marksheet=@sample_marksheet.txt;type=text/plain"
$dupJson = $dupOutput | ConvertFrom-Json
Write-Host "Duplicate Submission Response: $($dupJson.message)" -ForegroundColor Yellow