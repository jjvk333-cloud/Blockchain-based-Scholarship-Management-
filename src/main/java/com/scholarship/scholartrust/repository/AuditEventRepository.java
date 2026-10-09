package com.scholarship.scholartrust.repository;

import com.scholarship.scholartrust.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByApplicationIdOrderByTimestampAsc(Long applicationId);
    List<AuditEvent> findTop50ByOrderByTimestampDesc();
}
