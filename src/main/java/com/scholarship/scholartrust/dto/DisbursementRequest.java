package com.scholarship.scholartrust.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;

public class DisbursementRequest {

    private BigDecimal amount;

    @JsonAlias({"wallet", "recipientWalletAddress", "recipientWallet", "walletAddress"})
    private String recipientWalletAddress;

    private String remarks;

    public DisbursementRequest() {
    }

    public DisbursementRequest(BigDecimal amount, String recipientWalletAddress, String remarks) {
        this.amount = amount;
        this.recipientWalletAddress = recipientWalletAddress;
        this.remarks = remarks;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRecipientWalletAddress() {
        return recipientWalletAddress;
    }

    public void setRecipientWalletAddress(String recipientWalletAddress) {
        this.recipientWalletAddress = recipientWalletAddress;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}