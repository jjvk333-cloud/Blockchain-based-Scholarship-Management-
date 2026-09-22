package com.scholarship.scholartrust.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scholarships")
public class Scholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_gpa", nullable = false, precision = 3, scale = 2)
    private BigDecimal minGpa;

    @Column(name = "max_annual_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxAnnualIncome;

    @Column(name = "grant_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grantAmount;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Scholarship() {
    }

    public Scholarship(String title, String description, BigDecimal minGpa, BigDecimal maxAnnualIncome,
                       BigDecimal grantAmount, LocalDateTime deadline, User createdBy) {
        this.title = title;
        this.description = description;
        this.minGpa = minGpa;
        this.maxAnnualIncome = maxAnnualIncome;
        this.grantAmount = grantAmount;
        this.deadline = deadline;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMinGpa() {
        return minGpa;
    }

    public void setMinGpa(BigDecimal minGpa) {
        this.minGpa = minGpa;
    }

    public BigDecimal getMaxAnnualIncome() {
        return maxAnnualIncome;
    }

    public void setMaxAnnualIncome(BigDecimal maxAnnualIncome) {
        this.maxAnnualIncome = maxAnnualIncome;
    }

    public BigDecimal getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
