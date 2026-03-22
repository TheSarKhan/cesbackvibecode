package com.ces.erp.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, Long entityId, String entityLabel, String action, String summary) {
        String performer = "Sistem";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            performer = auth.getName(); // email
        }
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .entityLabel(entityLabel)
                .action(action)
                .performedBy(performer)
                .performedAt(LocalDateTime.now())
                .summary(summary)
                .build();
        auditLogRepository.save(entry);
    }
}
