package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.BlockchainTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {
    Optional<BlockchainTransaction> findByTxHash(String txHash);
    List<BlockchainTransaction> findByApplicationId(Long applicationId);
    List<BlockchainTransaction> findTop50ByOrderByCreatedAtDesc();
}
