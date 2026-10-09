package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.entity.AuditEvent;
import com.scholarship.scholartrust.entity.BlockchainTransaction;
import com.scholarship.scholartrust.repository.BlockchainTransactionRepository;
import com.scholarship.scholartrust.service.ApplicationService;
import com.scholarship.scholartrust.service.AuditEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditTimelineController {

    private final AuditEventService auditEventService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;
    private final ApplicationService applicationService;

    public AuditTimelineController(AuditEventService auditEventService,
                                   BlockchainTransactionRepository blockchainTransactionRepository,
                                   ApplicationService applicationService) {
        this.auditEventService = auditEventService;
        this.blockchainTransactionRepository = blockchainTransactionRepository;
        this.applicationService = applicationService;
    }

    @GetMapping("/timeline/{applicationId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getApplicationTimeline(
            @PathVariable Long applicationId,
            Authentication authentication) {
        // IDOR check handled inside getApplicationById
        applicationService.getApplicationById(applicationId, authentication.getName(),
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        List<AuditEvent> timeline = auditEventService.getApplicationTimeline(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Timeline retrieved successfully", timeline));
    }

    @GetMapping("/transactions/{applicationId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BlockchainTransaction>>> getApplicationTransactions(
            @PathVariable Long applicationId,
            Authentication authentication) {
        applicationService.getApplicationById(applicationId, authentication.getName(),
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        List<BlockchainTransaction> txs = blockchainTransactionRepository.findByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Blockchain transactions retrieved successfully", txs));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getRecentAuditEvents() {
        List<AuditEvent> recent = auditEventService.getRecentEvents();
        return ResponseEntity.ok(ApiResponse.success("Recent audit events retrieved", recent));
    }

    @GetMapping("/telemetry/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BlockchainTransaction>>> getRecentTransactions() {
        List<BlockchainTransaction> recent = blockchainTransactionRepository.findTop50ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success("Recent blockchain transactions retrieved", recent));
    }
}
