Write-Host "=== Demonstrating Tamper Detection ===" -ForegroundColor Red
$file = Get-ChildItem "uploads/documents" -Filter "*marksheet*" | Select-Object -First 1
Write-Host "Simulating Malicious Modification on disk file: $($file.Name)" -ForegroundColor Yellow

# Backup original file content
$originalBytes = [System.IO.File]::ReadAllBytes($file.FullName)

# Tamper with 1 byte of the file
[System.IO.File]::AppendAllText($file.FullName, "TAMPERED_MODIFIED_DATA")

# Admin triggers verification
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body (@{email="admin@college.edu"; password="AdminPassword@123"} | ConvertTo-Json) -ContentType "application/json"
$adminHeaders = @{ "Authorization" = "Bearer $($adminAuth.data.token)" }
$tamperResult = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/applications/documents/1/verify-hash" -Method Post -Headers $adminHeaders

Write-Host "`nIntegrity Check Status: $($tamperResult.data.status)" -ForegroundColor Red
Write-Host "Original Stored Hash: $($tamperResult.data.recordedDatabaseHash)" -ForegroundColor Yellow
Write-Host "Recalculated Hash:   $($tamperResult.data.calculatedHash)" -ForegroundColor Red
Write-Host "Verdict: $($tamperResult.data.verdictDetails)" -ForegroundColor Magenta

# Restore original content for clean state
[System.IO.File]::WriteAllBytes($file.FullName, $originalBytes)
Write-Host "`nFile restored to authentic state." -ForegroundColor Green
$restoredResult = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/applications/documents/1/verify-hash" -Method Post -Headers $adminHeaders
Write-Host "Re-check after restore: $($restoredResult.data.status)" -ForegroundColor Green