package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.entity.AuditEvent;
import com.scholarship.scholartrust.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent recordEvent(Long applicationId, String action, String actorEmail,
                                  String actorRole, String oldValue, String newValue,
                                  String txHash, String details) {
        try {
            AuditEvent event = new AuditEvent(applicationId, action, actorEmail, actorRole,
                    oldValue, newValue, txHash, details);
            return auditEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Could not log audit event for app #{}: {}", applicationId, ex.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getApplicationTimeline(Long applicationId) {
        return auditEventRepository.findByApplicationIdOrderByTimestampAsc(applicationId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getRecentEvents() {
        return auditEventRepository.findTop50ByOrderByTimestampDesc();
    }
}
