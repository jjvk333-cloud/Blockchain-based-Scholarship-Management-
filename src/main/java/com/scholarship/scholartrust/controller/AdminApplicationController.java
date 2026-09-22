package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.*;
import com.scholarship.scholartrust.entity.ApplicationStatus;
import com.scholarship.scholartrust.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/applications")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminApplicationController {

    private final ApplicationService applicationService;

    public AdminApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getAllApplications(
            @RequestParam(value = "status", required = false) ApplicationStatus status) {
        List<ApplicationResponse> list = applicationService.getAllApplicationsForAdmin(status);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved for admin review", list));
    }

    @PostMapping("/documents/{documentId}/verify-hash")
    public ResponseEntity<ApiResponse<HashVerificationResponse>> verifyDocumentHash(@PathVariable Long documentId) {
        HashVerificationResponse response = applicationService.verifyDocumentIntegrity(documentId);
        return ResponseEntity.ok(ApiResponse.success("Document cryptographic verification completed", response));
    }

    @GetMapping("/{id}/verify-hash")
    public ResponseEntity<ApiResponse<HashVerificationResponse>> verifyApplicationHash(@PathVariable Long id) {
        HashVerificationResponse response = applicationService.verifyApplicationDocumentIntegrity(id);
        return ResponseEntity.ok(ApiResponse.success("Document cryptographic verification completed", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, request.getStatus(), request.getRemarks());
        return ResponseEntity.ok(ApiResponse.success("Application status updated to " + request.getStatus() + " on database & blockchain", response));
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<ApiResponse<ApplicationResponse>> disburseScholarship(
            @PathVariable Long id,
            @RequestBody(required = false) DisbursementRequest request) {
        BigDecimal amount = (request != null) ? request.getAmount() : null;
        String wallet = (request != null) ? request.getRecipientWalletAddress() : null;
        ApplicationResponse response = applicationService.disburseScholarship(id, amount, wallet);
        return ResponseEntity.ok(ApiResponse.success("Scholarship funds successfully disbursed to student wallet on blockchain!", response));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<BlockchainAuditResponse>> auditApplication(@PathVariable Long id) {
        BlockchainAuditResponse response = applicationService.auditApplicationOnChain(id);
        return ResponseEntity.ok(ApiResponse.success("Blockchain ledger audit report generated", response));
    }
}