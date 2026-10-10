package com.scholarship.scholartrust.config;

import com.scholarship.scholartrust.service.BlockchainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

@Configuration
public class BlockchainStartupVerificationConfig {

    private static final Logger log = LoggerFactory.getLogger(BlockchainStartupVerificationConfig.class);

    @Bean
    public CommandLineRunner verifyBlockchainConnection(BlockchainService blockchainService) {
        return args -> {
            log.info("==========================================================");
            log.info("🔍 ScholarTrust Blockchain Startup Verification");
            log.info("==========================================================");
            try {
                boolean connected = blockchainService.isConnected();
                BigInteger blockNumber = blockchainService.getLatestBlockNumber();
                String contractAddr = blockchainService.getContractAddress();
                String adminAddr = blockchainService.getAdminAddress();
                String bytecode = blockchainService.getContractCode();
                boolean bytecodeExists = bytecode != null && bytecode.length() > 2;

                log.info("  EVM Local Node Status: {}", connected ? "ONLINE" : "OFFLINE / UNREACHABLE");
                log.info("  Current Block Number : #{}", blockNumber != null ? blockNumber : "N/A");
                log.info("  Target Contract Addr : {}", contractAddr);
                log.info("  Bytecode Verified    : {}", bytecodeExists ? "YES (Valid Contract Bytecode Deployed)" : "NO (Bytecode missing at address!)");
                log.info("  Admin / Relayer Addr : {}", adminAddr);
                if (!bytecodeExists) {
                    log.warn("⚠️ Bytecode missing at {}! Ensure contracts/deploy.js has been executed.", contractAddr);
                }
                log.info("==========================================================");
            } catch (Exception ex) {
                log.warn("⚠️ Blockchain node verification encountered error: {}", ex.getMessage());
                log.warn("ScholarTrust backend will run, but on-chain transactions require an active Ganache node.");
                log.info("==========================================================");
            }
        };
    }
}
