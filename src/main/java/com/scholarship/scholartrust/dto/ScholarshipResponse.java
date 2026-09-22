package com.scholarship.scholartrust.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScholarshipResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal minGpa;
    private BigDecimal maxAnnualIncome;
    private BigDecimal grantAmount;
    private LocalDateTime deadline;
    private boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private Boolean eligible;
    private List<String> eligibilityReasons = new ArrayList<>();

    public ScholarshipResponse() {
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

    public String getName() {
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

    public BigDecimal getAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public BigDecimal getMaxIncome() {
        return maxAnnualIncome;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getEligibilityReasons() {
        return eligibilityReasons;
    }

    public void setEligibilityReasons(List<String> eligibilityReasons) {
        this.eligibilityReasons = eligibilityReasons;
    }
}