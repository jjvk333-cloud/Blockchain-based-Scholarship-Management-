package com.scholarship.scholartrust.dto;

import com.scholarship.scholartrust.entity.ApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApplicationResponse {
    private Long id;
    private Long scholarshipId;
    private String scholarshipTitle;   // primary name
    private String scholarshipName;    // alias for frontend compatibility
    private BigDecimal grantAmount;
    private BigDecimal amount;         // alias for frontend compatibility

    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String rollNumber;
    private String department;
    private BigDecimal gpa;
    private BigDecimal annualIncome;
    private BigDecimal annualFamilyIncome;
    private String walletAddress;

    private ApplicationStatus status;
    private String blockchainTxHash;
    private String adminRemarks;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    private List<DocumentResponse> documents = new ArrayList<>();
    private DisbursementInfo disbursement;

    public ApplicationResponse() {}

    // ---- Nested DTO for disbursement ----
    public static class DisbursementInfo {
        private BigDecimal amount;
        private String recipientAddress;
        private String transactionHash;
        private Long blockNumber;
        private LocalDateTime disbursedAt;

        public DisbursementInfo() {}
        public DisbursementInfo(BigDecimal amount, String recipientAddress, String transactionHash,
                                Long blockNumber, LocalDateTime disbursedAt) {
            this.amount = amount;
            this.recipientAddress = recipientAddress;
            this.transactionHash = transactionHash;
            this.blockNumber = blockNumber;
            this.disbursedAt = disbursedAt;
        }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getRecipientAddress() { return recipientAddress; }
        public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
        public String getTransactionHash() { return transactionHash; }
        public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
        public Long getBlockNumber() { return blockNumber; }
        public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }
        public LocalDateTime getDisbursedAt() { return disbursedAt; }
        public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getScholarshipId() { return scholarshipId; }
    public void setScholarshipId(Long scholarshipId) { this.scholarshipId = scholarshipId; }

    public String getScholarshipTitle() { return scholarshipTitle; }
    public void setScholarshipTitle(String scholarshipTitle) { this.scholarshipTitle = scholarshipTitle; }

    public String getScholarshipName() { return scholarshipName; }
    public void setScholarshipName(String scholarshipName) { this.scholarshipName = scholarshipName; }

    public BigDecimal getGrantAmount() { return grantAmount; }
    public void setGrantAmount(BigDecimal grantAmount) { this.grantAmount = grantAmount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public BigDecimal getAnnualFamilyIncome() { return annualFamilyIncome; }
    public void setAnnualFamilyIncome(BigDecimal annualFamilyIncome) {
        this.annualFamilyIncome = annualFamilyIncome;
        this.annualIncome = annualFamilyIncome;
    }

    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getBlockchainTxHash() { return blockchainTxHash; }
    public void setBlockchainTxHash(String blockchainTxHash) { this.blockchainTxHash = blockchainTxHash; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<DocumentResponse> getDocuments() { return documents; }
    public void setDocuments(List<DocumentResponse> documents) { this.documents = documents; }

    public DisbursementInfo getDisbursement() { return disbursement; }
    public void setDisbursement(DisbursementInfo disbursement) { this.disbursement = disbursement; }
}