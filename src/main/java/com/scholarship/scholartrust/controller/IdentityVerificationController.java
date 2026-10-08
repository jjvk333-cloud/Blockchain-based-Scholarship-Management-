package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.dto.IdentityVerificationResponse;
import com.scholarship.scholartrust.service.IdentityVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/identity")
@CrossOrigin(origins = "*")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    public IdentityVerificationController(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> getMyVerificationStatus(Authentication authentication) {
        IdentityVerificationResponse status = identityVerificationService.getVerificationStatus(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Identity verification status retrieved", status));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> uploadIdentityDocument(
            @RequestParam("institutionalIdNumber") String institutionalIdNumber,
            @RequestParam("idCard") MultipartFile idCard,
            Authentication authentication) {
        IdentityVerificationResponse response = identityVerificationService.submitIdentityDocuments(
                authentication.getName(), institutionalIdNumber, idCard);
        return ResponseEntity.ok(ApiResponse.success("Identity documents uploaded successfully. Awaiting verification.", response));
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>> getPendingVerifications() {
        List<IdentityVerificationResponse> pending = identityVerificationService.getAllPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success("Pending verifications retrieved", pending));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>> getAllVerifications() {
        List<IdentityVerificationResponse> all = identityVerificationService.getAllVerifications();
        return ResponseEntity.ok(ApiResponse.success("All verifications retrieved", all));
    }

    @PostMapping("/admin/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> reviewVerification(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String reason = (String) body.get("rejectionReason");
        IdentityVerificationResponse result = identityVerificationService.reviewVerification(
                id, approve, reason, authentication.getName());
        String msg = approve ? "Identity verification approved." : "Identity verification rejected.";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }
}
