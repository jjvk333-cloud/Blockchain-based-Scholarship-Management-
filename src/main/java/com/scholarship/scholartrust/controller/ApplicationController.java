package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.dto.ApplicationResponse;
import com.scholarship.scholartrust.dto.DisbursementReceiptResponse;
import com.scholarship.scholartrust.dto.HashVerificationResponse;
import com.scholarship.scholartrust.entity.Document;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import com.scholarship.scholartrust.service.ApplicationService;
import com.scholarship.scholartrust.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public ApplicationController(ApplicationService applicationService,
                                 FileStorageService fileStorageService,
                                 UserRepository userRepository,
                                 StudentProfileRepository studentProfileRepository) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @RequestParam("scholarshipId") Long scholarshipId,
            @RequestParam("marksheet") MultipartFile marksheet,
            @RequestParam(value = "incomeCertificate", required = false) MultipartFile incomeCertificate,
            @RequestParam(value = "idProof", required = false) MultipartFile idProof,
            @RequestParam(value = "otherDocument", required = false) MultipartFile otherDocument,
            @RequestParam(value = "gpa", required = false) BigDecimal gpa,
            @RequestParam(value = "annualIncome", required = false) BigDecimal annualIncome,
            @RequestParam(value = "personalStatement", required = false) String personalStatement,
            Authentication authentication) {

        MultipartFile other = (idProof != null && !idProof.isEmpty()) ? idProof : otherDocument;

        // If GPA or income provided, update student profile
        if (gpa != null || annualIncome != null) {
            userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                studentProfileRepository.findByUser(user).ifPresent(profile -> {
                    if (gpa != null) profile.setGpa(gpa);
                    if (annualIncome != null) profile.setAnnualFamilyIncome(annualIncome);
                    studentProfileRepository.save(profile);
                });
            });
        }

        ApplicationResponse response = applicationService.submitApplication(
                scholarshipId,
                marksheet,
                incomeCertificate,
                other,
                authentication.getName(),
                personalStatement
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scholarship application submitted and documents cryptographically hashed successfully!", response));
    }

    @GetMapping({"/my-applications", "/my"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(Authentication authentication) {
        List<ApplicationResponse> list = applicationService.getApplicationsByStudent(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Your applications retrieved", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(@PathVariable Long id,
                                                                               Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ApplicationResponse response = applicationService.getApplicationById(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved", response));
    }

    @GetMapping("/{id}/verify-hash")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HashVerificationResponse>> verifyApplicationHash(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        HashVerificationResponse response = applicationService.verifyApplicationDocumentIntegrity(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Document integrity verified", response));
    }

    @GetMapping("/documents/{documentId}/verify-hash")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HashVerificationResponse>> verifySpecificDocumentHash(
            @PathVariable Long documentId,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        HashVerificationResponse response = applicationService.verifyDocumentIntegrity(documentId, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Document integrity verified", response));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Document doc = applicationService.getDocumentEntity(documentId, authentication.getName(), isAdmin);
        Resource resource = fileStorageService.loadFileAsResource(doc.getFilePath());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DisbursementReceiptResponse>> getReceipt(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        DisbursementReceiptResponse receipt = applicationService.getDisbursementReceipt(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Disbursement receipt retrieved successfully", receipt));
    }

    @GetMapping(value = "/{id}/receipt/html", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getReceiptHtml(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        DisbursementReceiptResponse r = applicationService.getDisbursementReceipt(id, authentication.getName(), isAdmin);

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>ScholarTrust - Disbursement Receipt</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; color: #1e293b; padding: 40px; }
                    .receipt-card { max-width: 750px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); padding: 36px; border: 1px solid #e2e8f0; }
                    .header { text-align: center; border-bottom: 2px dashed #cbd5e1; padding-bottom: 20px; margin-bottom: 24px; }
                    .logo { font-size: 26px; font-weight: 800; color: #2563eb; letter-spacing: -0.5px; }
                    .sub { color: #64748b; font-size: 13px; margin-top: 4px; }
                    .badge { display: inline-block; background: #dcfce7; color: #166534; padding: 6px 14px; border-radius: 9999px; font-weight: 600; font-size: 13px; margin-top: 12px; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; font-size: 14px; }
                    .field { margin-bottom: 8px; }
                    .label { color: #64748b; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 2px; }
                    .val { font-weight: 600; color: #0f172a; word-break: break-all; }
                    .highlight-box { background: #f1f5f9; border-left: 4px solid #2563eb; padding: 14px; border-radius: 4px; margin-bottom: 20px; font-family: monospace; font-size: 12px; word-break: break-all; }
                    .footer { text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px; }
                    @media print { body { background: white; padding: 0; } .receipt-card { box-shadow: none; border: none; } button { display: none; } }
                </style>
            </head>
            <body>
                <div class="receipt-card">
                    <div class="header">
                        <div class="logo">ScholarTrust</div>
                        <div class="sub">Blockchain-Verified Scholarship Management & Disbursement</div>
                        <div class="badge">CONFIRMED ON IMMUTABLE LEDGER</div>
                    </div>
                    
                    <div class="grid">
                        <div class="field">
                            <div class="label">Receipt Number</div>
                            <div class="val">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">Disbursement Date</div>
                            <div class="val">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">Student Name</div>
                            <div class="val">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">Student Email</div>
                            <div class="val">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">Roll Number / Dept</div>
                            <div class="val">%s (%s)</div>
                        </div>
                        <div class="field">
                            <div class="label">Scholarship Scheme</div>
                            <div class="val">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">Sanctioned Grant Amount</div>
                            <div class="val">&#8377; %s</div>
                        </div>
                        <div class="field">
                            <div class="label">Amount Disbursed</div>
                            <div class="val" style="color: #16a34a; font-size: 16px;">&#8377; %s</div>
                        </div>
                    </div>

                    <div class="label">Recipient Ethereum Wallet Address</div>
                    <div class="highlight-box">%s</div>

                    <div class="label">Blockchain Transaction Hash (TxHash)</div>
                    <div class="highlight-box">%s</div>

                    <div class="grid" style="font-size: 12px; margin-bottom: 12px;">
                        <div><span style="color: #64748b;">Block Number:</span> <b>%d</b></div>
                        <div><span style="color: #64748b;">Network:</span> <b>%s</b></div>
                    </div>

                    <div class="label">Verified Marksheet SHA-256 Hash</div>
                    <div class="highlight-box">%s</div>

                    <div style="text-align: center; margin-top: 20px;">
                        <button onclick="window.print()" style="background: #2563eb; color: white; border: none; padding: 10px 24px; border-radius: 6px; font-weight: 600; cursor: pointer;">Print / Save Receipt</button>
                    </div>

                    <div class="footer">
                        This receipt is cryptographically secured and verifiable against the ScholarTrust smart contract on the blockchain.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                HtmlUtils.htmlEscape(r.getReceiptNumber() != null ? r.getReceiptNumber() : ""),
                HtmlUtils.htmlEscape(r.getDisbursedAt() != null ? r.getDisbursedAt().toString() : "N/A"),
                HtmlUtils.htmlEscape(r.getStudentName() != null ? r.getStudentName() : ""),
                HtmlUtils.htmlEscape(r.getStudentEmail() != null ? r.getStudentEmail() : ""),
                HtmlUtils.htmlEscape(r.getRollNumber() != null ? r.getRollNumber() : "N/A"),
                HtmlUtils.htmlEscape(r.getDepartment() != null ? r.getDepartment() : "N/A"),
                HtmlUtils.htmlEscape(r.getScholarshipTitle() != null ? r.getScholarshipTitle() : ""),
                HtmlUtils.htmlEscape(r.getGrantAmount() != null ? r.getGrantAmount().toPlainString() : "0.00"),
                HtmlUtils.htmlEscape(r.getDisbursedAmount() != null ? r.getDisbursedAmount().toPlainString() : "0.00"),
                HtmlUtils.htmlEscape(r.getRecipientWallet() != null ? r.getRecipientWallet() : ""),
                HtmlUtils.htmlEscape(r.getBlockchainTxHash() != null ? r.getBlockchainTxHash() : ""),
                r.getBlockNumber() != null ? r.getBlockNumber() : 0L,
                HtmlUtils.htmlEscape(r.getNetwork() != null ? r.getNetwork() : ""),
                HtmlUtils.htmlEscape(r.getVerifiedDocumentHash() != null ? r.getVerifiedDocumentHash() : "")
        );

        return ResponseEntity.ok(html);
    }
}
