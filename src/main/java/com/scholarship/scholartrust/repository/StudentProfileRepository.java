package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    Optional<StudentProfile> findByRollNumber(String rollNumber);
    boolean existsByRollNumber(String rollNumber);
}
