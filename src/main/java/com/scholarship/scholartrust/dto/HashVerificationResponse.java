package com.scholarship.scholartrust.dto;

import com.scholarship.scholartrust.entity.DocumentType;
import java.time.LocalDateTime;

public class HashVerificationResponse {
    private Long documentId;
    private DocumentType documentType;
    private String fileName;
    private String calculatedHash;
    private String recordedDatabaseHash;
    private String status; // "MATCH" or "TAMPERED"
    private boolean match;
    private String verdictDetails;
    private LocalDateTime verifiedAt;

    public HashVerificationResponse() {
        this.verifiedAt = LocalDateTime.now();
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCalculatedHash() {
        return calculatedHash;
    }

    public void setCalculatedHash(String calculatedHash) {
        this.calculatedHash = calculatedHash;
    }

    public String getRecordedDatabaseHash() {
        return recordedDatabaseHash;
    }

    public void setRecordedDatabaseHash(String recordedDatabaseHash) {
        this.recordedDatabaseHash = recordedDatabaseHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getVerdictDetails() {
        return verdictDetails;
    }

    public void setVerdictDetails(String verdictDetails) {
        this.verdictDetails = verdictDetails;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}