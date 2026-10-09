package com.scholarship.scholartrust.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blockchain_transactions")
public class BlockchainTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_hash", length = 66, unique = true)
    private String txHash;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // RECORD_APPLICATION, UPDATE_STATUS, DISBURSE

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "gas_used")
    private Long gasUsed;

    @Column(name = "execution_latency_ms")
    private Long executionLatencyMs;

    @Column(name = "from_address", length = 42)
    private String fromAddress;

    @Column(name = "to_contract_address", length = 42)
    private String toContractAddress;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, REVERTED, TIMEOUT

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BlockchainTransaction() {}

    public BlockchainTransaction(String txHash, String actionType, Long applicationId,
                                 Long blockNumber, Long gasUsed, Long executionLatencyMs,
                                 String fromAddress, String toContractAddress, String status) {
        this.txHash = txHash;
        this.actionType = actionType;
        this.applicationId = applicationId;
        this.blockNumber = blockNumber;
        this.gasUsed = gasUsed;
        this.executionLatencyMs = executionLatencyMs;
        this.fromAddress = fromAddress;
        this.toContractAddress = toContractAddress;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }

    public Long getGasUsed() { return gasUsed; }
    public void setGasUsed(Long gasUsed) { this.gasUsed = gasUsed; }

    public Long getExecutionLatencyMs() { return executionLatencyMs; }
    public void setExecutionLatencyMs(Long executionLatencyMs) { this.executionLatencyMs = executionLatencyMs; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getToContractAddress() { return toContractAddress; }
    public void setToContractAddress(String toContractAddress) { this.toContractAddress = toContractAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
