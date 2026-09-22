package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.Application;
import com.scholarship.scholartrust.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {
    Optional<Disbursement> findByApplication(Application application);
    Optional<Disbursement> findByTransactionHash(String transactionHash);
}
