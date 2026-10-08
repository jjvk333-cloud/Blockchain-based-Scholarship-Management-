package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.StudentIdentityVerification;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentIdentityVerificationRepository extends JpaRepository<StudentIdentityVerification, Long> {
    Optional<StudentIdentityVerification> findByUser(User user);
    Optional<StudentIdentityVerification> findByUserId(Long userId);
    List<StudentIdentityVerification> findByStatus(VerificationStatus status);
}
