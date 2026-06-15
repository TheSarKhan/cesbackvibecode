package com.ces.erp.common.audit;

import com.ces.erp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG:GET')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime toDt   = to   != null ? LocalDate.parse(to).atTime(23, 59, 59) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        String qLower = (q != null && !q.isBlank()) ? q.toLowerCase() : null;
        String etFilter = (entityType != null && !entityType.isBlank()) ? entityType : null;
        String acFilter = (action     != null && !action.isBlank())     ? action     : null;

        Page<AuditLog> result = auditLogRepository.findFiltered(
                etFilter, acFilter, qLower, fromDt, toDt,
                PageRequest.of(page, size)
        );

        Map<String, Object> body = Map.of(
                "content",       result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages",    result.getTotalPages(),
                "page",          result.getNumber()
        );
        return ResponseEntity.ok(ApiResponse.success("Audit jurnal", body));
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.success("Tarixçə", auditLogRepository.findRecent()));
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success("Tarixçə",
                auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId)));
    }
}
