package com.scholarship.scholartrust.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursements")
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    @JsonIgnore
    private Application application;

    @Column(name = "student_wallet", nullable = false, length = 42)
    private String studentWallet;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisbursementStatus status = DisbursementStatus.CONFIRMED;

    @Column(name = "disbursed_at", nullable = false, updatable = false)
    private LocalDateTime disbursedAt;

    public Disbursement() {
    }

    public Disbursement(Application application, String studentWallet, BigDecimal amount,
                        String transactionHash, Long blockNumber) {
        this.application = application;
        this.studentWallet = studentWallet;
        this.amount = amount;
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
    }

    @PrePersist
    protected void onCreate() {
        this.disbursedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getStudentWallet() {
        return studentWallet;
    }

    public void setStudentWallet(String studentWallet) {
        this.studentWallet = studentWallet;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public DisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(DisbursementStatus status) {
        this.status = status;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public String getRecipientAddress() {
        return studentWallet;
    }
}
