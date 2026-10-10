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
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final ApplicationService applicationService;

    public BlockchainController(BlockchainService blockchainService, ApplicationService applicationService) {
        this.blockchainService = blockchainService;
        this.applicationService = applicationService;
    }

    @GetMapping("/network")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNetworkDetails(org.springframework.security.core.Authentication authentication) {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean isConnected = blockchainService.isConnected();
        BigInteger blockNumber = isConnected ? blockchainService.getLatestBlockNumber() : null;
        String contractCode = isConnected ? blockchainService.getContractCode() : null;
        boolean hasBytecode = contractCode != null && contractCode.length() > 2;

        details.put("connected", isConnected);
        details.put("blockNumber", blockNumber);
        details.put("latestBlockNumber", blockNumber);
        details.put("networkId", "1337 (Ganache)");
        details.put("network", "Local Ethereum Node (Ganache)");
        details.put("contractAddress", blockchainService.getContractAddress());
        details.put("hasContractBytecode", hasBytecode);

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String rawAdmin = blockchainService.getAdminAddress();
        String maskedAdmin = (rawAdmin != null && rawAdmin.length() >= 10)
                ? rawAdmin.substring(0, 6) + "..." + rawAdmin.substring(rawAdmin.length() - 4)
                : "0x...";

        if (isAdmin) {
            details.put("adminAddress", rawAdmin);
            details.put("adminRelayerAddress", rawAdmin);
        } else {
            details.put("adminAddress", maskedAdmin);
            details.put("adminRelayerAddress", maskedAdmin);
        }

        details.put("status", (isConnected && hasBytecode) ? "SYNCED & ACTIVE" : (isConnected ? "CONNECTED (NO CONTRACT)" : "DISCONNECTED"));

        return ResponseEntity.ok(ApiResponse.success("Blockchain network status retrieved", details));
    }

    @GetMapping("/audit/{applicationId}")
    public ResponseEntity<ApiResponse<BlockchainAuditResponse>> auditApplication(@PathVariable Long applicationId) {
        BlockchainAuditResponse response = applicationService.auditApplicationOnChain(applicationId);
        return ResponseEntity.ok(ApiResponse.success("On-chain audit completed", response));
    }
}
