package com.scholarship.scholartrust.dto;

import com.scholarship.scholartrust.entity.DocumentType;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private DocumentType documentType;
    private String fileName;
    private Long fileSize;
    private String sha256Hash;
    private LocalDateTime uploadedAt;
    private String downloadUrl;

    public DocumentResponse() {
    }

    public DocumentResponse(Long id, DocumentType documentType, String fileName, Long fileSize,
                            String sha256Hash, LocalDateTime uploadedAt, String downloadUrl) {
        this.id = id;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sha256Hash = sha256Hash;
        this.uploadedAt = uploadedAt;
        this.downloadUrl = downloadUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}