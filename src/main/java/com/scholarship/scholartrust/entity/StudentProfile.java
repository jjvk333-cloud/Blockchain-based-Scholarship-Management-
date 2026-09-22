package com.scholarship.scholartrust.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "roll_number", unique = true, length = 50)
    private String rollNumber;

    @Column(length = 100)
    private String department;

    @Column(precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(name = "annual_family_income", precision = 12, scale = 2)
    private BigDecimal annualFamilyIncome;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;

    @Column(length = 20)
    private String phone;

    public StudentProfile() {
    }

    public StudentProfile(User user, String rollNumber, String department, BigDecimal gpa,
                          BigDecimal annualFamilyIncome, String walletAddress, String phone) {
        this.user = user;
        this.rollNumber = rollNumber;
        this.department = department;
        this.gpa = gpa;
        this.annualFamilyIncome = annualFamilyIncome;
        this.walletAddress = walletAddress;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }

    public BigDecimal getAnnualFamilyIncome() { return annualFamilyIncome; }
    public void setAnnualFamilyIncome(BigDecimal annualFamilyIncome) { this.annualFamilyIncome = annualFamilyIncome; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}