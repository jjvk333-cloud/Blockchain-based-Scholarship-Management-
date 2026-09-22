Write-Host "=== 1. Admin Login & Create New Scholarship ===" -ForegroundColor Cyan
$adminLogin = @{ email = "admin@college.edu"; password = "AdminPassword@123" } | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminAuth.data.token

$schReq = @{
    title = "Prime Minister National STEM Excellence Grant 2026"
    description = "Full tuition grant for top computer science students."
    minGpa = 8.00
    maxAnnualIncome = 400000.00
    grantAmount = 75000.00
    deadline = "2026-12-31 23:59:59"
} | ConvertTo-Json
$newSch = Invoke-RestMethod -Uri "http://localhost:8080/api/scholarships" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $schReq -ContentType "application/json"
$schId = $newSch.data.id
Write-Host "Created Scholarship ID: $schId ($($newSch.data.title))" -ForegroundColor Green

Write-Host "`n=== 2. Student Submits Application & Anchors on Blockchain ===" -ForegroundColor Cyan
$studLogin = @{ email = "student@college.edu"; password = "Password@123" } | ConvertTo-Json
$studAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $studLogin -ContentType "application/json"
$studToken = $studAuth.data.token

# Create fresh files
"Official Semester Grade Report - GPA 8.75 - Verified" | Set-Content "grade_sheet.txt" -Encoding UTF8
"Tehsildar Certified Income Proof - Rs 2,50,000" | Set-Content "income_cert.txt" -Encoding UTF8

$curlOut = & curl.exe -s -X POST "http://localhost:8080/api/applications/apply" `
  -H "Authorization: Bearer $studToken" `
  -F "scholarshipId=$schId" `
  -F "marksheet=@grade_sheet.txt;type=text/plain" `
  -F "incomeCertificate=@income_cert.txt;type=text/plain"

$appJson = $curlOut | ConvertFrom-Json
$appId = $appJson.data.id
Write-Host "Application Submitted! App ID: $appId" -ForegroundColor Green
Write-Host "Blockchain Transaction Hash: $($appJson.data.blockchainTxHash)" -ForegroundColor Yellow
$marksheetHash = $appJson.data.documents[0].sha256Hash
Write-Host "Anchored Document SHA-256: $marksheetHash" -ForegroundColor Cyan

Write-Host "`n=== 3. Audit On-Chain State from Smart Contract ===" -ForegroundColor Cyan
$auditBefore = Invoke-RestMethod -Uri "http://localhost:8080/api/blockchain/audit/$appId" -Method Get
Write-Host "Exists on Blockchain: $($auditBefore.data.existsOnChain)" -ForegroundColor Green
Write-Host "On-Chain Status:      $($auditBefore.data.onChainStatus)" -ForegroundColor Yellow
Write-Host "On-Chain Doc Hash:    $($auditBefore.data.onChainDocHash)" -ForegroundColor Cyan
Write-Host "Tamper-Free:          $($auditBefore.data.tamperFree)" -ForegroundColor Green

Write-Host "`n=== 4. Admin Approves Application ===" -ForegroundColor Cyan
$approveReq = @{ status = "APPROVED"; remarks = "Academic credentials and income proofs verified on blockchain." } | ConvertTo-Json
$approvedApp = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/applications/$appId/status" -Method Put -Headers @{ Authorization = "Bearer $adminToken" } -Body $approveReq -ContentType "application/json"
Write-Host "Status Updated: $($approvedApp.data.status)" -ForegroundColor Green

Write-Host "`n=== 5. Admin Executes On-Chain Disbursement ===" -ForegroundColor Cyan
$disbReq = @{ status = "DISBURSED"; remarks = "Approved by Dean. Transferring 75,000 scholarship credits." } | ConvertTo-Json
$disbResult = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/applications/$appId/disburse" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $disbReq -ContentType "application/json"
Write-Host "Disbursement Response: $($disbResult.message)" -ForegroundColor Green
Write-Host "New Application Status: $($disbResult.data.status)" -ForegroundColor Yellow

Write-Host "`n=== 6. Final Blockchain Ledger Audit ===" -ForegroundColor Cyan
$finalAudit = Invoke-RestMethod -Uri "http://localhost:8080/api/blockchain/audit/$appId" -Method Get
Write-Host "On-Chain Status:    $($finalAudit.data.onChainStatus)" -ForegroundColor Green
Write-Host "Disbursed On-Chain: $($finalAudit.data.disbursed)" -ForegroundColor Green
Write-Host "Disbursed Amount:   ₹$($finalAudit.data.disbursedAmount)" -ForegroundColor Yellow
Write-Host "Disbursement Tx:    $($finalAudit.data.disbursementTxHash)" -ForegroundColor Cyan
Write-Host "Audit Verdict:      $($finalAudit.data.auditVerdict)" -ForegroundColor Magenta