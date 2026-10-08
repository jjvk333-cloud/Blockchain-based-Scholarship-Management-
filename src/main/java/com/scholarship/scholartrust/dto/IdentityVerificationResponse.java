package com.scholarship.scholartrust.dto;

import com.scholarship.scholartrust.entity.VerificationStatus;
import java.time.LocalDateTime;

public class IdentityVerificationResponse {
    private Long id;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private String institutionalIdNumber;
    private String idCardFilePath;
    private String idCardSha256;
    private VerificationStatus status;
    private String verifiedByEmail;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public IdentityVerificationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getInstitutionalIdNumber() { return institutionalIdNumber; }
    public void setInstitutionalIdNumber(String institutionalIdNumber) { this.institutionalIdNumber = institutionalIdNumber; }

    public String getIdCardFilePath() { return idCardFilePath; }
    public void setIdCardFilePath(String idCardFilePath) { this.idCardFilePath = idCardFilePath; }

    public String getIdCardSha256() { return idCardSha256; }
    public void setIdCardSha256(String idCardSha256) { this.idCardSha256 = idCardSha256; }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public String getVerifiedByEmail() { return verifiedByEmail; }
    public void setVerifiedByEmail(String verifiedByEmail) { this.verifiedByEmail = verifiedByEmail; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
