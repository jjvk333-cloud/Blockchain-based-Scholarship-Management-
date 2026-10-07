package com.scholarship.scholartrust.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "scholarship_id"})
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(name = "personal_statement", columnDefinition = "TEXT")
    private String personalStatement;

    @Column(name = "submitted_roll_number", length = 50)
    private String submittedRollNumber;

    @Column(name = "submitted_department", length = 100)
    private String submittedDepartment;

    @Column(name = "submitted_gpa", precision = 3, scale = 2)
    private BigDecimal submittedGpa;

    @Column(name = "submitted_annual_income", precision = 12, scale = 2)
    private BigDecimal submittedAnnualIncome;

    @Column(name = "submitted_wallet_address", length = 42)
    private String submittedWalletAddress;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Disbursement disbursement;

    public Application() {
    }

    public Application(User student, Scholarship scholarship) {
        this.student = student;
        this.scholarship = scholarship;
    }

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Scholarship getScholarship() {
        return scholarship;
    }

    public void setScholarship(Scholarship scholarship) {
        this.scholarship = scholarship;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getPersonalStatement() {
        return personalStatement;
    }

    public void setPersonalStatement(String personalStatement) {
        this.personalStatement = personalStatement;
    }

    public String getSubmittedRollNumber() {
        return submittedRollNumber;
    }

    public void setSubmittedRollNumber(String submittedRollNumber) {
        this.submittedRollNumber = submittedRollNumber;
    }

    public String getSubmittedDepartment() {
        return submittedDepartment;
    }

    public void setSubmittedDepartment(String submittedDepartment) {
        this.submittedDepartment = submittedDepartment;
    }

    public BigDecimal getSubmittedGpa() {
        return submittedGpa;
    }

    public void setSubmittedGpa(BigDecimal submittedGpa) {
        this.submittedGpa = submittedGpa;
    }

    public BigDecimal getSubmittedAnnualIncome() {
        return submittedAnnualIncome;
    }

    public void setSubmittedAnnualIncome(BigDecimal submittedAnnualIncome) {
        this.submittedAnnualIncome = submittedAnnualIncome;
    }

    public String getSubmittedWalletAddress() {
        return submittedWalletAddress;
    }

    public void setSubmittedWalletAddress(String submittedWalletAddress) {
        this.submittedWalletAddress = submittedWalletAddress;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public void addDocument(Document document) {
        documents.add(document);
        document.setApplication(this);
    }

    public Disbursement getDisbursement() {
        return disbursement;
    }

    public void setDisbursement(Disbursement disbursement) {
        this.disbursement = disbursement;
        if (disbursement != null) {
            disbursement.setApplication(this);
        }
    }
}
