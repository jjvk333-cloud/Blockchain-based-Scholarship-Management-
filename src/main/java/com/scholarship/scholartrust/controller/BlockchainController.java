package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.dto.BlockchainAuditResponse;
import com.scholarship.scholartrust.service.ApplicationService;
import com.scholarship.scholartrust.service.BlockchainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blockchain")
@CrossOrigin(origins = "*")
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final ApplicationService applicationService;

    public BlockchainController(BlockchainService blockchainService, ApplicationService applicationService) {
        this.blockchainService = blockchainService;
        this.applicationService = applicationService;
    }

    @GetMapping("/network")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNetworkDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        BigInteger blockNumber = blockchainService.getLatestBlockNumber();
        boolean isConnected = blockNumber != null && blockNumber.compareTo(BigInteger.ZERO) >= 0;

        details.put("connected", isConnected);
        details.put("blockNumber", blockNumber);
        details.put("latestBlockNumber", blockNumber);
        details.put("networkId", "1337 (Ganache)");
        details.put("network", "Local Ethereum Node (Ganache)");
        details.put("contractAddress", blockchainService.getContractAddress());
        details.put("adminAddress", blockchainService.getAdminAddress());
        details.put("adminRelayerAddress", blockchainService.getAdminAddress());
        details.put("status", isConnected ? "SYNCED & ACTIVE" : "DISCONNECTED");

        return ResponseEntity.ok(ApiResponse.success("Blockchain network status retrieved", details));
    }

    @GetMapping("/audit/{applicationId}")
    public ResponseEntity<ApiResponse<BlockchainAuditResponse>> auditApplication(@PathVariable Long applicationId) {
        BlockchainAuditResponse response = applicationService.auditApplicationOnChain(applicationId);
        return ResponseEntity.ok(ApiResponse.success("On-chain audit completed", response));
    }
}