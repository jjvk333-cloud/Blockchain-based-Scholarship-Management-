package com.scholarship.scholartrust.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_identity_verifications")
public class StudentIdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "institutional_id_number", length = 50)
    private String institutionalIdNumber;

    @Column(name = "id_card_file_path", length = 500)
    private String idCardFilePath;

    @Column(name = "id_card_sha256", length = 64)
    private String idCardSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.UNVERIFIED;

    @Column(name = "verified_by_email", length = 100)
    private String verifiedByEmail;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public StudentIdentityVerification() {}

    public StudentIdentityVerification(User user) {
        this.user = user;
        this.status = VerificationStatus.UNVERIFIED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
