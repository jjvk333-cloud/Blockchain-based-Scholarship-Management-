package com.scholarship.scholartrust.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisbursementReceiptResponse {

    private String receiptNumber;
    private Long applicationId;
    private String studentName;
    private String studentEmail;
    private String rollNumber;
    private String department;
    private String scholarshipTitle;
    private String scholarshipCategory;
    private BigDecimal grantAmount;
    private BigDecimal disbursedAmount;
    private String recipientWallet;
    private String blockchainTxHash;
    private Long blockNumber;
    private String contractAddress;
    private String network;
    private LocalDateTime disbursedAt;
    private String status;
    private String verifiedDocumentHash;
    private String auditUrl;

    public DisbursementReceiptResponse() {}

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getScholarshipTitle() {
        return scholarshipTitle;
    }

    public void setScholarshipTitle(String scholarshipTitle) {
        this.scholarshipTitle = scholarshipTitle;
    }

    public String getScholarshipCategory() {
        return scholarshipCategory;
    }

    public void setScholarshipCategory(String scholarshipCategory) {
        this.scholarshipCategory = scholarshipCategory;
    }

    public BigDecimal getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public String getRecipientWallet() {
        return recipientWallet;
    }

    public void setRecipientWallet(String recipientWallet) {
        this.recipientWallet = recipientWallet;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerifiedDocumentHash() {
        return verifiedDocumentHash;
    }

    public void setVerifiedDocumentHash(String verifiedDocumentHash) {
        this.verifiedDocumentHash = verifiedDocumentHash;
    }

    public String getAuditUrl() {
        return auditUrl;
    }

    public void setAuditUrl(String auditUrl) {
        this.auditUrl = auditUrl;
    }
}
