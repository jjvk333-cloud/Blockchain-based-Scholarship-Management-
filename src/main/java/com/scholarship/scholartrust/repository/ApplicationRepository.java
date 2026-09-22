package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.Application;
import com.scholarship.scholartrust.entity.ApplicationStatus;
import com.scholarship.scholartrust.entity.Scholarship;
import com.scholarship.scholartrust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudent(User student);
    boolean existsByStudentAndScholarship(User student, Scholarship scholarship);
    List<Application> findByStatus(ApplicationStatus status);
}
