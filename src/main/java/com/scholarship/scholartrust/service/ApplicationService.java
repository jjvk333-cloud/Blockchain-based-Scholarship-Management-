package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.ApplicationResponse;
import com.scholarship.scholartrust.dto.BlockchainAuditResponse;
import com.scholarship.scholartrust.dto.DisbursementReceiptResponse;
import com.scholarship.scholartrust.dto.DocumentResponse;
import com.scholarship.scholartrust.dto.HashVerificationResponse;
import com.scholarship.scholartrust.entity.*;
import com.scholarship.scholartrust.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final ScholarshipRepository scholarshipRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DocumentRepository documentRepository;
    private final DisbursementRepository disbursementRepository;
    private final FileStorageService fileStorageService;
    private final BlockchainService blockchainService;
    private final ScholarshipService scholarshipService;
    private final IdentityVerificationService identityVerificationService;
    private final AuditEventService auditEventService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ScholarshipRepository scholarshipRepository,
                              UserRepository userRepository,
                              StudentProfileRepository studentProfileRepository,
                              DocumentRepository documentRepository,
                              DisbursementRepository disbursementRepository,
                              FileStorageService fileStorageService,
                              BlockchainService blockchainService,
                              ScholarshipService scholarshipService,
                              IdentityVerificationService identityVerificationService,
                              AuditEventService auditEventService) {
        this.applicationRepository = applicationRepository;
        this.scholarshipRepository = scholarshipRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.documentRepository = documentRepository;
        this.disbursementRepository = disbursementRepository;
        this.fileStorageService = fileStorageService;
        this.blockchainService = blockchainService;
        this.scholarshipService = scholarshipService;
        this.identityVerificationService = identityVerificationService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public ApplicationResponse submitApplication(Long scholarshipId,
                                                 MultipartFile marksheet,
                                                 MultipartFile incomeCertificate,
                                                 MultipartFile otherDocument,
                                                 String studentEmail) {
        return submitApplication(scholarshipId, marksheet, incomeCertificate, otherDocument, studentEmail, null);
    }

    @Transactional
    public ApplicationResponse submitApplication(Long scholarshipId,
                                                 MultipartFile marksheet,
                                                 MultipartFile incomeCertificate,
                                                 MultipartFile otherDocument,
                                                 String studentEmail,
                                                 String personalStatement) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Scholarship scholarship = scholarshipRepository.findById(scholarshipId)
                .orElseThrow(() -> new IllegalArgumentException("Scholarship not found with ID: " + scholarshipId));

        if (applicationRepository.existsByStudentAndScholarship(student, scholarship)) {
            throw new IllegalStateException("You have already applied for this scholarship.");
        }

        if (marksheet == null || marksheet.isEmpty()) {
            throw new IllegalArgumentException("Marksheet document is required.");
        }

        StudentProfile profile = studentProfileRepository.findByUser(student)
                .orElseThrow(() -> new IllegalStateException("Student profile not found. Please complete your academic profile before applying."));

        // Evaluate eligibility strictly using unified evaluator
        ScholarshipService.EligibilityResult eligibility = scholarshipService.evaluateEligibility(scholarship, profile);
        if (!eligibility.isEligible()) {
            throw new IllegalArgumentException("Eligibility requirements not met: " + String.join(" ", eligibility.getReasons()));
        }

        if (!identityVerificationService.isStudentVerified(student)) {
            throw new IllegalStateException("Institutional identity verification required. Please submit your ID card and await verification before applying.");
        }

        String walletAddr = profile.getWalletAddress();
        if (walletAddr == null || !walletAddr.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalStateException("A valid registered Ethereum wallet address is required before applying.");
        }

        // Create Application with Snapshot
        Application application = new Application(student, scholarship);
        application.setStatus(ApplicationStatus.PENDING);
        application.setPersonalStatement(personalStatement);
        application.setSubmittedRollNumber(profile.getRollNumber());
        application.setSubmittedDepartment(profile.getDepartment());
        application.setSubmittedGpa(profile.getGpa());
        application.setSubmittedAnnualIncome(profile.getAnnualFamilyIncome());
        application.setSubmittedWalletAddress(walletAddr);

        Application savedApp = applicationRepository.save(application);

        // Process & Hash Marksheet
        Document marksheetDoc = saveDocument(savedApp, marksheet, DocumentType.MARKSHEET);

        // Process Optional Documents
        if (incomeCertificate != null && !incomeCertificate.isEmpty()) {
            saveDocument(savedApp, incomeCertificate, DocumentType.INCOME_CERTIFICATE);
        }
        if (otherDocument != null && !otherDocument.isEmpty()) {
            saveDocument(savedApp, otherDocument, DocumentType.ID_PROOF);
        }

        // Anchor Application & Marksheet Hash on Blockchain
        try {
            String txHash = blockchainService.recordApplicationOnChain(
                    savedApp.getId(),
                    walletAddr,
                    scholarship.getId(),
                    marksheetDoc.getSha256Hash()
            );
            savedApp.setBlockchainTxHash(txHash);
            savedApp.setBlockchainOperationStatus(BlockchainOperationStatus.CONFIRMED);
            savedApp.setBlockchainError(null);
            applicationRepository.save(savedApp);
        } catch (Exception e) {
            log.error("Failed to anchor application #{} on blockchain: {}", savedApp.getId(), e.getMessage());
            savedApp.setBlockchainOperationStatus(BlockchainOperationStatus.FAILED);
            savedApp.setBlockchainError("Blockchain anchoring failed: " + e.getMessage());
            applicationRepository.save(savedApp);
        }

        if (auditEventService != null) {
            String auditDesc = savedApp.getBlockchainOperationStatus() == BlockchainOperationStatus.CONFIRMED
                    ? "Application submitted with documents. Marksheet hash anchored on blockchain."
                    : "Application submitted to database, but blockchain anchoring failed: " + savedApp.getBlockchainError();
            auditEventService.recordEvent(
                    savedApp.getId(), "APPLICATION_SUBMITTED", student.getEmail(), "STUDENT",
                    null, ApplicationStatus.PENDING.name(), savedApp.getBlockchainTxHash(),
                    auditDesc
            );
        }

        return mapToResponse(savedApp);
    }

    private Document saveDocument(Application app, MultipartFile file, DocumentType docType) {
        String sha256Hash = fileStorageService.calculateSha256(file);
        String savedPath = fileStorageService.storeFile(file, app.getId(), docType);

        Document doc = new Document(
                docType,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                savedPath,
                file.getSize(),
                sha256Hash
        );
        app.addDocument(doc);
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStudent(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return applicationRepository.findByStudent(student).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long applicationId, String userEmail, boolean isAdmin) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (!isAdmin && !application.getStudent().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not authorized to view this application.");
        }

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicationsForAdmin(ApplicationStatus status) {
        List<Application> list = (status != null)
                ? applicationRepository.findByStatus(status)
                : applicationRepository.findAll();
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HashVerificationResponse verifyApplicationDocumentIntegrity(Long applicationId) {
        return verifyApplicationDocumentIntegrity(applicationId, null, true);
    }

    @Transactional(readOnly = true)
    public HashVerificationResponse verifyApplicationDocumentIntegrity(Long applicationId, String requestingEmail, boolean isAdmin) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Authorization check to prevent IDOR
        if (!isAdmin && (requestingEmail == null || !app.getStudent().getEmail().equalsIgnoreCase(requestingEmail))) {
            throw new AccessDeniedException("Access denied: You do not have permission to verify documents for application " + applicationId);
        }

        // Find the primary marksheet document
        Document doc = app.getDocuments().stream()
                .filter(d -> d.getDocumentType() == DocumentType.MARKSHEET)
                .findFirst()
                .orElse(app.getDocuments().isEmpty() ? null : app.getDocuments().get(0));

        if (doc == null) {
            HashVerificationResponse r = new HashVerificationResponse();
            r.setStatus("NO_DOCUMENTS");
            r.setVerdictDetails("No documents found for this application.");
            return r;
        }

        Path filePath = fileStorageService.resolvePath(doc.getFilePath());
        String recalculatedHash = fileStorageService.calculateSha256(filePath);

        HashVerificationResponse response = new HashVerificationResponse();
        response.setDocumentId(doc.getId());
        response.setDocumentType(doc.getDocumentType());
        response.setFileName(doc.getFileName());
        response.setCalculatedHash(recalculatedHash);
        response.setRecordedDatabaseHash(doc.getSha256Hash());

        boolean diskMatch = recalculatedHash.equalsIgnoreCase(doc.getSha256Hash());
        String chainResult = blockchainService.verifyHashOnChainResult(applicationId, recalculatedHash);

        if ("BLOCKCHAIN_UNAVAILABLE".equalsIgnoreCase(chainResult)) {
            response.setMatch(false);
            response.setStatus("BLOCKCHAIN_UNAVAILABLE");
            response.setVerdictDetails("Cryptographic verification incomplete: Blockchain node is currently unavailable. Stored database record is " + (diskMatch ? "intact on disk" : "tampered on disk") + ".");
            return response;
        }

        if ("NOT_ON_CHAIN".equalsIgnoreCase(chainResult)) {
            response.setMatch(false);
            response.setStatus("NOT_ON_CHAIN");
            response.setVerdictDetails("Application is not yet recorded on the blockchain ledger. Database file is " + (diskMatch ? "intact on disk" : "tampered on disk") + ".");
            return response;
        }

        if ("INCONCLUSIVE".equalsIgnoreCase(chainResult)) {
            response.setMatch(false);
            response.setStatus("INCONCLUSIVE");
            response.setVerdictDetails("Blockchain verification inconclusive due to RPC execution warning.");
            return response;
        }

        boolean chainMatch = "VERIFIED".equalsIgnoreCase(chainResult);
        boolean match = diskMatch && chainMatch;
        response.setMatch(match);

        if (diskMatch && chainMatch) {
            response.setStatus("MATCH");
            response.setVerdictDetails("File integrity verified. Hash matches both database and blockchain records.");
        } else if (!diskMatch) {
            response.setStatus("TAMPERED");
            response.setVerdictDetails("TAMPER DETECTED: File on disk has been modified! Hash does not match stored records.");
        } else {
            response.setStatus("TAMPERED");
            response.setVerdictDetails("TAMPER DETECTED: File matches database but does NOT match blockchain hash!");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public HashVerificationResponse verifyDocumentIntegrity(Long documentId) {
        return verifyDocumentIntegrity(documentId, null, true);
    }

    @Transactional(readOnly = true)
    public HashVerificationResponse verifyDocumentIntegrity(Long documentId, String requestingEmail, boolean isAdmin) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        Application app = doc.getApplication();
        if (!isAdmin && (requestingEmail == null || !app.getStudent().getEmail().equalsIgnoreCase(requestingEmail))) {
            throw new AccessDeniedException("Access denied: You do not have permission to verify document " + documentId);
        }

        Path filePath = fileStorageService.resolvePath(doc.getFilePath());
        String recalculatedHash = fileStorageService.calculateSha256(filePath);

        HashVerificationResponse response = new HashVerificationResponse();
        response.setDocumentId(doc.getId());
        response.setDocumentType(doc.getDocumentType());
        response.setFileName(doc.getFileName());
        response.setCalculatedHash(recalculatedHash);
        response.setRecordedDatabaseHash(doc.getSha256Hash());

        boolean diskMatch = recalculatedHash.equalsIgnoreCase(doc.getSha256Hash());

        // Check against on-chain anchor for this application
        String chainResult = blockchainService.verifyHashOnChainResult(app.getId(), recalculatedHash);
        if ("BLOCKCHAIN_UNAVAILABLE".equalsIgnoreCase(chainResult)) {
            response.setMatch(false);
            response.setStatus("BLOCKCHAIN_UNAVAILABLE");
            response.setVerdictDetails("Blockchain node is unavailable. Stored database record is " + (diskMatch ? "intact on disk." : "tampered on disk."));
            return response;
        }

        if ("NOT_ON_CHAIN".equalsIgnoreCase(chainResult)) {
            response.setMatch(false);
            response.setStatus("NOT_ON_CHAIN");
            response.setVerdictDetails("Application is not yet recorded on the blockchain ledger. File is " + (diskMatch ? "intact on disk." : "tampered on disk."));
            return response;
        }

        boolean chainMatch = "VERIFIED".equalsIgnoreCase(chainResult);
        boolean match = diskMatch && (chainMatch || (doc.getDocumentType() != DocumentType.MARKSHEET && diskMatch));
        response.setMatch(match);

        if (diskMatch && chainMatch) {
            response.setStatus("MATCH");
            response.setVerdictDetails("Document integrity verified on disk and anchored on-chain.");
        } else if (diskMatch && doc.getDocumentType() != DocumentType.MARKSHEET) {
            response.setStatus("MATCH");
            response.setVerdictDetails("Supporting document integrity verified against database record.");
        } else if (!diskMatch) {
            response.setStatus("TAMPERED");
            response.setVerdictDetails("TAMPER DETECTED: Document modified on disk!");
        } else {
            response.setStatus("TAMPERED");
            response.setVerdictDetails("TAMPER DETECTED: Hash does not match on-chain anchor!");
        }

        return response;
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatus newStatus, String remarks) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        ApplicationStatus currentStatus = application.getStatus();
        if (currentStatus == ApplicationStatus.DISBURSED) {
            throw new IllegalStateException("Cannot change status of an already DISBURSED application.");
        }
        if (currentStatus == ApplicationStatus.REJECTED && newStatus != ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot change status of an already REJECTED application.");
        }
        if (newStatus == ApplicationStatus.DISBURSED) {
            throw new IllegalArgumentException("Use the dedicated disbursement endpoint to disburse funds.");
        }

        // Handle on-chain state machine:
        // Smart contract requires: SUBMITTED (0) -> UNDER_REVIEW (1) -> APPROVED (2) or REJECTED (3)
        try {
            if (currentStatus == ApplicationStatus.PENDING && newStatus == ApplicationStatus.APPROVED) {
                // Step through UNDER_REVIEW first so smart contract state machine is satisfied
                blockchainService.updateStatusOnChain(applicationId, ApplicationStatus.REVIEWING.toChainOrdinal());
                String txHash = blockchainService.updateStatusOnChain(applicationId, newStatus.toChainOrdinal());
                application.setBlockchainTxHash(txHash);
            } else if (currentStatus == ApplicationStatus.PENDING && newStatus == ApplicationStatus.REJECTED) {
                // Step through UNDER_REVIEW first
                blockchainService.updateStatusOnChain(applicationId, ApplicationStatus.REVIEWING.toChainOrdinal());
                String txHash = blockchainService.updateStatusOnChain(applicationId, newStatus.toChainOrdinal());
                application.setBlockchainTxHash(txHash);
            } else {
                String txHash = blockchainService.updateStatusOnChain(applicationId, newStatus.toChainOrdinal());
                application.setBlockchainTxHash(txHash);
            }
            application.setBlockchainOperationStatus(BlockchainOperationStatus.CONFIRMED);
            application.setBlockchainError(null);
        } catch (Exception e) {
            log.error("Blockchain status update failed for application #{}: {}", applicationId, e.getMessage());
            application.setBlockchainOperationStatus(BlockchainOperationStatus.FAILED);
            application.setBlockchainError("Blockchain status update failed: " + e.getMessage());
            // Do not claim blockchain success; fail the operation cleanly so caller knows
            throw new IllegalStateException("Failed to update status on blockchain: " + e.getMessage() + ". Database state was not altered.");
        }

        application.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            application.setAdminRemarks(remarks);
        }

        Application saved = applicationRepository.save(application);

        if (auditEventService != null) {
            auditEventService.recordEvent(
                    applicationId, "STATUS_CHANGED", "admin", "ADMIN",
                    currentStatus.name(), newStatus.name(), saved.getBlockchainTxHash(),
                    "Application status changed to " + newStatus.name() + (remarks != null ? ". Remarks: " + remarks : "")
            );
        }

        return mapToResponse(saved);
    }

    @Transactional
    public ApplicationResponse disburseScholarship(Long applicationId, String remarks) {
        return disburseScholarship(applicationId, null, null);
    }

    @Transactional
    public ApplicationResponse disburseScholarship(Long applicationId, BigDecimal amount, String recipientWallet) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (application.getStatus() == ApplicationStatus.DISBURSED) {
            throw new IllegalStateException("Scholarship has already been disbursed for this application.");
        }

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application must be APPROVED before disbursement. Current status: " + application.getStatus());
        }

        // Use provided values or fall back to scholarship defaults
        BigDecimal grantAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                ? amount : application.getScholarship().getGrantAmount();

        StudentProfile profile = studentProfileRepository.findByUser(application.getStudent()).orElse(null);
        String walletAddr = null;
        try {
            List<Type> chainData = blockchainService.getApplicationFromChain(applicationId);
            if (chainData != null && chainData.size() > 1) {
                String chainAddr = ((Address) chainData.get(1)).getValue();
                if (chainAddr != null && !chainAddr.equalsIgnoreCase("0x0000000000000000000000000000000000000000")) {
                    walletAddr = chainAddr;
                }
            }
        } catch (Exception ignored) {}

        if (walletAddr == null) {
            if (recipientWallet != null && !recipientWallet.isBlank()) {
                walletAddr = recipientWallet.trim();
            } else if (profile != null && profile.getWalletAddress() != null && !profile.getWalletAddress().isBlank()) {
                walletAddr = profile.getWalletAddress().trim();
            }
        }

        // Strict Validation: Remove silent fallback wallet. Enforce 0x + 40 hex chars
        if (walletAddr == null || !walletAddr.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalArgumentException("Disbursement rejected: Student does not have a registered valid Ethereum wallet address (must be 0x followed by 40 hex characters).");
        }

        // Execute on-chain disbursement
        TransactionReceipt receipt;
        try {
            receipt = blockchainService.disburseScholarshipOnChain(
                    application.getId(), walletAddr, grantAmount);
        } catch (Exception e) {
            throw new RuntimeException("Blockchain disbursement failed: " + e.getMessage() + ". Application status remains APPROVED and was NOT marked as DISBURSED.", e);
        }

        if (receipt == null || !receipt.isStatusOK()) {
            throw new RuntimeException("Blockchain disbursement transaction reverted or failed on-chain. Application remains APPROVED.");
        }

        String txHash = receipt.getTransactionHash();
        Long blockNumber = receipt.getBlockNumber().longValue();

        Disbursement disbursement = new Disbursement(
                application, walletAddr, grantAmount, txHash, blockNumber);
        disbursementRepository.save(disbursement);

        application.setStatus(ApplicationStatus.DISBURSED);
        application.setBlockchainTxHash(txHash);
        applicationRepository.save(application);

        if (auditEventService != null) {
            auditEventService.recordEvent(
                    applicationId, "SCHOLARSHIP_DISBURSED", "admin", "ADMIN",
                    ApplicationStatus.APPROVED.name(), ApplicationStatus.DISBURSED.name(), txHash,
                    "Disbursed amount ₹" + grantAmount + " to wallet " + walletAddr + " on block #" + blockNumber
            );
        }

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public BlockchainAuditResponse auditApplicationOnChain(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        BlockchainAuditResponse audit = new BlockchainAuditResponse();
        audit.setApplicationId(applicationId);
        audit.setBlockchainTxHash(app.getBlockchainTxHash());

        String dbStatus = app.getStatus().name();
        audit.setDbStatus(dbStatus);

        String dbHash = app.getDocuments().stream()
                .filter(d -> d.getDocumentType() == DocumentType.MARKSHEET)
                .findFirst()
                .map(Document::getSha256Hash)
                .orElse("N/A");
        audit.setDatabaseDocHash(dbHash);

        try {
            List<Type> chainData = blockchainService.getApplicationFromChain(applicationId);

            if (chainData == null || chainData.isEmpty() || !((Bool) chainData.get(6)).getValue()) {
                audit.setExistsOnChain(false);
                audit.setTamperFree(false);
                audit.setBlockchainStatus("NOT_ON_CHAIN");
                audit.setHashMatch(false);
                audit.setAuditVerdict("Application is NOT yet recorded on the blockchain ledger.");
                return audit;
            }

            audit.setExistsOnChain(true);
            audit.setStudentAddress(((Address) chainData.get(1)).getValue());
            audit.setScholarshipId(((Uint256) chainData.get(2)).getValue().longValue());

            String onChainHash = ((Utf8String) chainData.get(3)).getValue();
            audit.setOnChainDocHash(onChainHash);

            int statusOrdinal = ((Uint8) chainData.get(4)).getValue().intValue();
            String chainStatus = ApplicationStatus.fromChainOrdinal(statusOrdinal).name();
            audit.setBlockchainStatus(chainStatus);
            audit.setOnChainStatus(chainStatus);

            long timestampSeconds = ((Uint256) chainData.get(5)).getValue().longValue();
            audit.setBlockTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampSeconds), ZoneId.systemDefault()));

            boolean hashMatch = onChainHash.equalsIgnoreCase(dbHash);
            audit.setTamperFree(hashMatch);
            audit.setHashMatch(hashMatch);

            List<Type> disbData = blockchainService.getDisbursementFromChain(applicationId);
            if (!disbData.isEmpty() && ((Bool) disbData.get(4)).getValue()) {
                audit.setDisbursed(true);
                audit.setDisbursedAmount(new BigDecimal(((Uint256) disbData.get(2)).getValue()));
                disbursementRepository.findByApplication(app).ifPresent(d ->
                        audit.setDisbursementTxHash(d.getTransactionHash()));
            } else {
                audit.setDisbursed(false);
            }

            audit.setAuditVerdict(hashMatch
                    ? "VERIFIED: Blockchain and database records match."
                    : "TAMPER ALERT: Database does NOT match the immutable blockchain ledger!");

        } catch (Exception e) {
            audit.setExistsOnChain(false);
            audit.setBlockchainStatus("BLOCKCHAIN_UNAVAILABLE");
            audit.setHashMatch(false);
            audit.setAuditVerdict("Could not reach blockchain node. Is Ganache running?");
        }

        return audit;
    }

    @Transactional(readOnly = true)
    public Document getDocumentEntity(Long documentId) {
        return getDocumentEntity(documentId, null, true);
    }

    @Transactional(readOnly = true)
    public Document getDocumentEntity(Long documentId, String requestingEmail, boolean isAdmin) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!isAdmin && (requestingEmail == null || !doc.getApplication().getStudent().getEmail().equalsIgnoreCase(requestingEmail))) {
            throw new AccessDeniedException("Access denied: You do not have permission to access document " + documentId);
        }
        return doc;
    }

    @Transactional(readOnly = true)
    public DisbursementReceiptResponse getDisbursementReceipt(Long applicationId, String requestingEmail, boolean isAdmin) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (!isAdmin && (requestingEmail == null || !app.getStudent().getEmail().equalsIgnoreCase(requestingEmail))) {
            throw new AccessDeniedException("Access denied: You do not have permission to view the disbursement receipt for application " + applicationId);
        }

        if (app.getStatus() != ApplicationStatus.DISBURSED) {
            throw new IllegalStateException("Application has not been disbursed yet. Current status: " + app.getStatus());
        }

        Disbursement d = disbursementRepository.findByApplication(app)
                .orElseThrow(() -> new IllegalStateException("Disbursement record not found for application: " + applicationId));

        StudentProfile profile = studentProfileRepository.findByUser(app.getStudent()).orElse(null);

        String docHash = app.getDocuments().stream()
                .filter(doc -> doc.getDocumentType() == DocumentType.MARKSHEET)
                .findFirst()
                .map(Document::getSha256Hash)
                .orElse("N/A");

        DisbursementReceiptResponse r = new DisbursementReceiptResponse();
        r.setReceiptNumber("REC-" + app.getId() + "-" + (d.getBlockNumber() != null ? d.getBlockNumber() : "ETH"));
        r.setApplicationId(app.getId());
        r.setStudentName(app.getStudent().getFullName());
        r.setStudentEmail(app.getStudent().getEmail());
        r.setRollNumber(profile != null ? profile.getRollNumber() : "N/A");
        r.setDepartment(profile != null ? profile.getDepartment() : "N/A");
        r.setScholarshipTitle(app.getScholarship().getTitle());
        r.setScholarshipCategory("MERIT / NEED-BASED");
        r.setGrantAmount(app.getScholarship().getGrantAmount());
        r.setDisbursedAmount(d.getAmount());
        r.setRecipientWallet(d.getStudentWallet());
        r.setBlockchainTxHash(d.getTransactionHash());
        r.setBlockNumber(d.getBlockNumber());
        r.setContractAddress(blockchainService.getContractAddress());
        r.setNetwork("Ganache Local Ethereum Ledger (Chain ID 1337)");
        r.setDisbursedAt(d.getDisbursedAt());
        r.setStatus("DISBURSED & VERIFIED ON IMMUTABLE BLOCKCHAIN");
        r.setVerifiedDocumentHash(docHash);
        r.setAuditUrl("/api/blockchain/audit/" + app.getId());

        return r;
    }

    private ApplicationResponse mapToResponse(Application app) {
        ApplicationResponse dto = new ApplicationResponse();
        dto.setId(app.getId());
        dto.setScholarshipId(app.getScholarship().getId());

        // Support both field names: scholarshipTitle and scholarshipName
        String title = app.getScholarship().getTitle();
        dto.setScholarshipTitle(title);
        dto.setScholarshipName(title);   // alias for frontend compatibility

        dto.setGrantAmount(app.getScholarship().getGrantAmount());
        dto.setAmount(app.getScholarship().getGrantAmount());  // alias

        User student = app.getStudent();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFullName());
        dto.setStudentEmail(student.getEmail());

        studentProfileRepository.findByUser(student).ifPresent(p -> {
            dto.setRollNumber(p.getRollNumber());
            dto.setDepartment(p.getDepartment());
            dto.setGpa(p.getGpa());
            dto.setAnnualFamilyIncome(p.getAnnualFamilyIncome());
            dto.setWalletAddress(p.getWalletAddress());
        });

        // Set snapshot fields and personal statement
        dto.setPersonalStatement(app.getPersonalStatement());
        dto.setSubmittedRollNumber(app.getSubmittedRollNumber());
        dto.setSubmittedDepartment(app.getSubmittedDepartment());
        dto.setSubmittedGpa(app.getSubmittedGpa());
        dto.setSubmittedAnnualIncome(app.getSubmittedAnnualIncome());
        dto.setSubmittedWalletAddress(app.getSubmittedWalletAddress());

        dto.setStatus(app.getStatus());
        dto.setBlockchainTxHash(app.getBlockchainTxHash());
        dto.setBlockchainOperationStatus(app.getBlockchainOperationStatus());
        dto.setBlockchainError(app.getBlockchainError());
        dto.setAdminRemarks(app.getAdminRemarks());
        dto.setAppliedAt(app.getAppliedAt());
        dto.setUpdatedAt(app.getUpdatedAt());

        // Disbursement
        disbursementRepository.findByApplication(app).ifPresent(d -> {
            dto.setDisbursement(new ApplicationResponse.DisbursementInfo(
                    d.getAmount(), d.getStudentWallet(), d.getTransactionHash(),
                    d.getBlockNumber(), d.getDisbursedAt()));
        });

        List<DocumentResponse> docList = app.getDocuments().stream().map(d -> new DocumentResponse(
                d.getId(),
                d.getDocumentType(),
                d.getFileName(),
                d.getFileSize(),
                d.getSha256Hash(),
                d.getUploadedAt(),
                "/api/applications/documents/" + d.getId() + "/download"
        )).collect(Collectors.toList());

        dto.setDocuments(docList);
        return dto;
    }
}