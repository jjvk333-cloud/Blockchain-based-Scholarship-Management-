package com.scholarship.scholartrust.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BlockchainAuditResponse {
    private Long applicationId;
    private String studentAddress;
    private Long scholarshipId;
    private String onChainDocHash;
    private String databaseDocHash;
    private String onChainStatus;
    private String blockchainStatus;   // alias for frontend
    private String dbStatus;           // DB status for comparison
    private boolean hashMatch;         // direct field for frontend
    private LocalDateTime blockTimestamp;
    private String blockchainTxHash;
    private boolean existsOnChain;
    private boolean tamperFree;
    private boolean disbursed;
    private BigDecimal disbursedAmount;
    private String disbursementTxHash;
    private String auditVerdict;

    public BlockchainAuditResponse() {}

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getStudentAddress() { return studentAddress; }
    public void setStudentAddress(String studentAddress) { this.studentAddress = studentAddress; }

    public Long getScholarshipId() { return scholarshipId; }
    public void setScholarshipId(Long scholarshipId) { this.scholarshipId = scholarshipId; }

    public String getOnChainDocHash() { return onChainDocHash; }
    public void setOnChainDocHash(String onChainDocHash) { this.onChainDocHash = onChainDocHash; }

    public String getDatabaseDocHash() { return databaseDocHash; }
    public void setDatabaseDocHash(String databaseDocHash) { this.databaseDocHash = databaseDocHash; }

    public String getOnChainStatus() { return onChainStatus; }
    public void setOnChainStatus(String onChainStatus) { this.onChainStatus = onChainStatus; }

    public String getBlockchainStatus() { return blockchainStatus; }
    public void setBlockchainStatus(String blockchainStatus) { this.blockchainStatus = blockchainStatus; }

    public String getDbStatus() { return dbStatus; }
    public void setDbStatus(String dbStatus) { this.dbStatus = dbStatus; }

    public boolean isHashMatch() { return hashMatch; }
    public void setHashMatch(boolean hashMatch) { this.hashMatch = hashMatch; }

    public LocalDateTime getBlockTimestamp() { return blockTimestamp; }
    public void setBlockTimestamp(LocalDateTime blockTimestamp) { this.blockTimestamp = blockTimestamp; }

    public String getBlockchainTxHash() { return blockchainTxHash; }
    public void setBlockchainTxHash(String blockchainTxHash) { this.blockchainTxHash = blockchainTxHash; }

    public boolean isExistsOnChain() { return existsOnChain; }
    public void setExistsOnChain(boolean existsOnChain) { this.existsOnChain = existsOnChain; }

    public boolean isTamperFree() { return tamperFree; }
    public void setTamperFree(boolean tamperFree) { this.tamperFree = tamperFree; }

    public boolean isDisbursed() { return disbursed; }
    public void setDisbursed(boolean disbursed) { this.disbursed = disbursed; }

    public BigDecimal getDisbursedAmount() { return disbursedAmount; }
    public void setDisbursedAmount(BigDecimal disbursedAmount) { this.disbursedAmount = disbursedAmount; }

    public String getDisbursementTxHash() { return disbursementTxHash; }
    public void setDisbursementTxHash(String disbursementTxHash) { this.disbursementTxHash = disbursementTxHash; }

    public String getAuditVerdict() { return auditVerdict; }
    public void setAuditVerdict(String auditVerdict) { this.auditVerdict = auditVerdict; }
}