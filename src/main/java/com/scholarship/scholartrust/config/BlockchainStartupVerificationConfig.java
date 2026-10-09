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
                BigInteger blockNumber = blockchainService.getLatestBlockNumber();
                String contractAddr = blockchainService.getContractAddress();
                String adminAddr = blockchainService.getAdminAddress();

                log.info("  EVM Local Node Status: ONLINE");
                log.info("  Current Block Number : #{}", blockNumber);
                log.info("  Target Contract Addr : {}", contractAddr);
                log.info("  Admin / Relayer Addr : {}", adminAddr);
                log.info("==========================================================");
            } catch (Exception ex) {
                log.warn("⚠️ Blockchain node is currently unavailable or unreachable: {}", ex.getMessage());
                log.warn("ScholarTrust backend will run, but on-chain transactions require an active Ganache node.");
                log.info("==========================================================");
            }
        };
    }
}
