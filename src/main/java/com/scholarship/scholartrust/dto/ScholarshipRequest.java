package com.scholarship.scholartrust.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScholarshipRequest {

    @NotBlank(message = "Title is required")
    @JsonAlias({"name", "scholarshipName"})
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @DecimalMin(value = "0.0", message = "Min GPA cannot be negative")
    @DecimalMax(value = "10.0", message = "Min GPA cannot exceed 10.0")
    private BigDecimal minGpa = BigDecimal.ZERO;

    @Positive(message = "Maximum annual income must be positive")
    @JsonAlias({"maxIncome"})
    private BigDecimal maxAnnualIncome = new BigDecimal("99999999.00");

    @NotNull(message = "Grant amount is required")
    @Positive(message = "Grant amount must be positive")
    @JsonAlias({"amount"})
    private BigDecimal grantAmount;

    @NotNull(message = "Deadline is required")
    private LocalDateTime deadline;

    public ScholarshipRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return title;
    }

    public void setName(String name) {
        if (this.title == null || this.title.isBlank()) {
            this.title = name;
        }
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

    public BigDecimal getMaxIncome() {
        return maxAnnualIncome;
    }

    public void setMaxIncome(BigDecimal maxIncome) {
        if (this.maxAnnualIncome == null) {
            this.maxAnnualIncome = maxIncome;
        }
    }

    public BigDecimal getGrantAmount() {
        return grantAmount;
    }

    public void setGrantAmount(BigDecimal grantAmount) {
        this.grantAmount = grantAmount;
    }

    public BigDecimal getAmount() {
        return grantAmount;
    }

    public void setAmount(BigDecimal amount) {
        if (this.grantAmount == null) {
            this.grantAmount = amount;
        }
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(Object deadlineObj) {
        if (deadlineObj == null) {
            this.deadline = null;
            return;
        }
        if (deadlineObj instanceof LocalDateTime ldt) {
            this.deadline = ldt;
            return;
        }
        String str = deadlineObj.toString().trim();
        try {
            if (str.length() == 10) { // yyyy-MM-dd
                this.deadline = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
            } else if (str.contains("T")) {
                this.deadline = LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (str.contains(" ")) {
                this.deadline = LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                this.deadline = LocalDate.parse(str).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            this.deadline = LocalDate.parse(str.substring(0, 10)).atTime(23, 59, 59);
        }
    }
}