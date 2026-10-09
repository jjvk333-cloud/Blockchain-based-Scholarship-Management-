# ScholarTrust End-to-End Hardened Verification Test Suite (Conference-Grade Edition)
$baseUrl = "http://localhost:8080"
$passCount = 0
$failCount = 0

function Assert-Test($condition, $testName) {
    if ($condition) {
        Write-Host " [PASS] $testName" -ForegroundColor Green
        $script:passCount++
    } else {
        Write-Host " [FAIL] $testName" -ForegroundColor Red
        $script:failCount++
    }
}

Write-Host "=== SCHOLARTRUST HARDENED E2E TEST SUITE ===" -ForegroundColor Cyan

# 1. Blockchain Network Check
try {
    $bc = Invoke-RestMethod -Uri "$baseUrl/api/blockchain/network" -Method GET
    Assert-Test ($bc.success -eq $true -and $bc.data.connected -eq $true) "1. Blockchain network connected to Ganache"
} catch {
    Assert-Test $false "1. Blockchain network check failed: $_"
}

# 2. Security Test: Reject Public Admin Registration
try {
    $adminPayload = @{
        email = "attacker_admin@college.edu"
        password = "Password123"
        fullName = "Attacker"
        role = "ROLE_ADMIN"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $adminPayload -ContentType "application/json"
    Assert-Test $false "2. Public admin registration MUST be rejected"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test ($statusCode -eq 400) "2. Security: Public admin registration rejected with HTTP 400"
}

# 3. Public Student Registration
$rand = Get-Random -Minimum 100000 -Maximum 999999
$studentEmail = "student.$rand@college.edu"
$studentWallet = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
try {
    $regPayload = @{
        email = $studentEmail
        password = "StudentPassword123"
        fullName = "Venkatesh Student $rand"
        rollNumber = "CS$rand"
        department = "Computer Science"
        gpa = 8.85
        annualFamilyIncome = 180000
        walletAddress = $studentWallet
        phone = "9876543210"
    } | ConvertTo-Json
    $regRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $regPayload -ContentType "application/json"
    $studentToken = $regRes.data.token
    Assert-Test ($regRes.success -eq $true -and $studentToken -ne $null -and $regRes.data.role -eq "ROLE_STUDENT") "3. Student registered successfully with ROLE_STUDENT"
} catch {
    Assert-Test $false "3. Student registration failed: $_"
}

# 4. Student Profile Fetch & Update
try {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $profRes = Invoke-RestMethod -Uri "$baseUrl/api/student/profile" -Method GET -Headers $headers
    Assert-Test ($profRes.data.email -eq $studentEmail -and $profRes.data.walletAddress -eq $studentWallet) "4. Student profile retrieved via /api/student/profile"

    $updPayload = @{
        phone = "9123456780"
        department = "Artificial Intelligence & Data Science"
        gpa = 9.10
        annualFamilyIncome = 150000
        walletAddress = $studentWallet
    } | ConvertTo-Json
    $updRes = Invoke-RestMethod -Uri "$baseUrl/api/student/profile" -Method PUT -Headers $headers -Body $updPayload -ContentType "application/json"
    Assert-Test ($updRes.data.gpa -eq 9.10 -and $updRes.data.department -eq "Artificial Intelligence & Data Science") "5. Student profile updated successfully via /api/student/profile"
} catch {
    Assert-Test $false "4/5. Student profile operations failed: $_"
}

# 5. Institutional Identity Verification (IDENT-01 Workflow)
$idCardFile = "V:\Projects\scholartrust\uploads\test_idcard_$rand.txt"
"STUDENT INSTITUTIONAL ID CARD PROOF: CS$rand - College of Engineering" | Set-Content $idCardFile
try {
    $idUploadOut = & curl.exe -s -X POST "$baseUrl/api/identity/upload" `
        -H "Authorization: Bearer $studentToken" `
        -F "institutionalIdNumber=CS$rand" `
        -F "idCard=@$idCardFile;type=text/plain"
    $idUploadRes = $idUploadOut | ConvertFrom-Json
    $idVerifId = $idUploadRes.data.id
    Assert-Test ($idUploadRes.success -eq $true -and $idUploadRes.data.status -eq "PENDING") "6. Student submitted institutional ID card (PENDING verification)"

    # Admin Login to verify student
    $adminLogin = @{
        email = "admin@college.edu"
        password = "Admin@123"
    } | ConvertTo-Json
    $adminRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $adminLogin -ContentType "application/json"
    $adminToken = $adminRes.data.token
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }

    # Admin Reviews and Approves Student Identity
    $reviewBody = @{
        approve = $true
    } | ConvertTo-Json
    $reviewRes = Invoke-RestMethod -Uri "$baseUrl/api/identity/admin/$idVerifId/review" -Method POST -Headers $adminHeaders -Body $reviewBody -ContentType "application/json"
    Assert-Test ($reviewRes.data.status -eq "VERIFIED") "7. Admin approved student institutional identity (VERIFIED)"
} catch {
    Assert-Test $false "6/7. Institutional identity verification workflow failed: $_"
}

# 6. Scholarship Eligibility Check
try {
    $eligRes = Invoke-RestMethod -Uri "$baseUrl/api/scholarships/1/check-eligibility" -Method GET -Headers $headers
    Assert-Test ($eligRes.data.eligible -eq $true) "8. Student checked eligibility: Eligible = true"
} catch {
    Assert-Test $false "8. Eligibility check failed: $_"
}

# 7. Apply with Document Upload
$sampleDoc = "V:\Projects\scholartrust\uploads\test_doc_$rand.txt"
"ScholarTrust Official Verification Document - Student $rand - GPA 9.10" | Set-Content $sampleDoc
try {
    $curlOut = & curl.exe -s -X POST "$baseUrl/api/applications/apply" `
        -H "Authorization: Bearer $studentToken" `
        -F "scholarshipId=1" `
        -F "marksheet=@$sampleDoc;type=text/plain" `
        -F "gpa=9.10" `
        -F "annualIncome=150000" `
        -F "personalStatement=Passionate about decentralized systems and research."
    $appRes = $curlOut | ConvertFrom-Json
    $appId = $appRes.data.id
    $docId = $appRes.data.documents[0].id
    $docHash = $appRes.data.documents[0].sha256Hash
    Assert-Test ($appRes.success -eq $true -and $appId -gt 0 -and $docHash.Length -eq 64 -and $appRes.data.personalStatement -ne $null) "9. Scholarship application submitted with snapshot & personal statement anchored"
} catch {
    Assert-Test $false "9. Application submit failed: $_"
}

# 8. IDOR Security Test: Another Student tries to download or verify Document
try {
    $otherReg = @{
        email = "other.$rand@college.edu"
        password = "Password123"
        fullName = "Other Student"
        rollNumber = "OT$rand"
    } | ConvertTo-Json
    $otherRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $otherReg -ContentType "application/json"
    $otherToken = $otherRes.data.token
    $otherHeaders = @{ Authorization = "Bearer $otherToken" }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/applications/documents/$docId/download" -Method GET -Headers $otherHeaders
        Assert-Test $false "10. IDOR check: unauthorized student MUST NOT download document"
    } catch {
        $sc = $_.Exception.Response.StatusCode.value__
        Assert-Test ($sc -eq 403) "10. Security: IDOR prevented on /documents/$docId/download (HTTP 403 Forbidden)"
    }

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/applications/$appId/verify-hash" -Method GET -Headers $otherHeaders
        Assert-Test $false "11. IDOR check: unauthorized student MUST NOT verify hash"
    } catch {
        $sc = $_.Exception.Response.StatusCode.value__
        Assert-Test ($sc -eq 403) "11. Security: IDOR prevented on /$appId/verify-hash (HTTP 403 Forbidden)"
    }
} catch {
    Assert-Test $false "10/11. IDOR test setup failed: $_"
}

# 9. Admin Login & Hash Verification
try {
    # Admin verifies hash
    $verifyRes = Invoke-RestMethod -Uri "$baseUrl/api/applications/$appId/verify-hash" -Method GET -Headers $adminHeaders
    Assert-Test ($verifyRes.data.match -eq $true -and $verifyRes.data.status -eq "MATCH") "12. Admin verified document hash integrity against blockchain (MATCH)"
} catch {
    Assert-Test $false "12. Admin verify failed: $_"
}

# 10. Admin Approves Application
try {
    $statusBody = @{
        status = "APPROVED"
        remarks = "Documents verified and hash validated on-chain by Dean."
    } | ConvertTo-Json
    $apprRes = Invoke-RestMethod -Uri "$baseUrl/api/admin/applications/$appId/status" -Method PUT -Headers $adminHeaders -Body $statusBody -ContentType "application/json"
    Assert-Test ($apprRes.data.status -eq "APPROVED") "13. Admin approved application on DB and blockchain"
} catch {
    Assert-Test $false "13. Admin approval failed: $_"
}

# 11. Admin Disburses Scholarship on Blockchain
try {
    $disbBody = @{
        amount = 50000
        recipientWallet = $studentWallet
    } | ConvertTo-Json
    $disbRes = Invoke-RestMethod -Uri "$baseUrl/api/admin/applications/$appId/disburse" -Method POST -Headers $adminHeaders -Body $disbBody -ContentType "application/json"
    $txHash = $disbRes.data.disbursement.transactionHash
    Assert-Test ($disbRes.data.status -eq "DISBURSED" -and $txHash -ne $null) "14. Blockchain disbursement executed on Ganache! TxHash: $txHash"
} catch {
    Assert-Test $false "14. Disbursement failed: $_"
}

# 12. Student Fetches Disbursement Receipt (JSON & HTML)
try {
    $receiptRes = Invoke-RestMethod -Uri "$baseUrl/api/applications/$appId/receipt" -Method GET -Headers $headers
    Assert-Test ($receiptRes.data.receiptNumber -ne $null -and $receiptRes.data.recipientWallet -eq $studentWallet) "15. Student fetched official disbursement receipt JSON"

    $htmlRes = Invoke-RestMethod -Uri "$baseUrl/api/applications/$appId/receipt/html" -Method GET -Headers $headers
    Assert-Test ($htmlRes -like "*ScholarTrust*" -and $htmlRes -like "*CONFIRMED ON IMMUTABLE LEDGER*") "16. Student fetched printable HTML disbursement receipt"
} catch {
    Assert-Test $false "15/16. Receipt fetch failed: $_"
}

# 13. Audit Event Timeline Verification (AUDIT-01)
try {
    $timelineRes = Invoke-RestMethod -Uri "$baseUrl/api/audit/timeline/$appId" -Method GET -Headers $headers
    $actions = $timelineRes.data | ForEach-Object { $_.action }
    $hasSubmitted = $actions -contains "APPLICATION_SUBMITTED"
    $hasApproved = $actions -contains "STATUS_CHANGED"
    $hasDisbursed = $actions -contains "SCHOLARSHIP_DISBURSED"
    Assert-Test ($timelineRes.success -eq $true -and $hasSubmitted -and $hasApproved -and $hasDisbursed) "17. Audit timeline verified complete lifecycle history ($($timelineRes.data.Count) events recorded)"
} catch {
    Assert-Test $false "17. Audit timeline verification failed: $_"
}

# 14. Blockchain Telemetry Verification (CHAIN-06)
try {
    $txTelemetryRes = Invoke-RestMethod -Uri "$baseUrl/api/audit/transactions/$appId" -Method GET -Headers $headers
    $telemetryCount = $txTelemetryRes.data.Count
    $firstTx = $txTelemetryRes.data[0]
    Assert-Test ($txTelemetryRes.success -eq $true -and $telemetryCount -ge 2 -and $firstTx.gasUsed -gt 0) "18. Blockchain transaction telemetry captured gas used ($($firstTx.gasUsed) gas, $($firstTx.executionLatencyMs) ms latency)"
} catch {
    Assert-Test $false "18. Blockchain telemetry verification failed: $_"
}

# 15. Public Blockchain Audit
try {
    $auditRes = Invoke-RestMethod -Uri "$baseUrl/api/blockchain/audit/$appId" -Method GET
    Assert-Test ($auditRes.data.tamperFree -eq $true -and $auditRes.data.disbursed -eq $true) "19. Public blockchain audit confirmed TAMPER-FREE and DISBURSED on Ganache"
} catch {
    Assert-Test $false "19. Blockchain audit failed: $_"
}

Write-Host "---------------------------------------------" -ForegroundColor Cyan
Write-Host "TEST SUMMARY: $passCount PASSED, $failCount FAILED" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
