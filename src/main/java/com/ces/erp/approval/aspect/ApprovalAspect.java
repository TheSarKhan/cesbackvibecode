package com.ces.erp.approval.aspect;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.approval.exception.PendingApprovalException;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.approval.service.ApprovalPersistenceService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.department.entity.Department;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalAspect {

    private final PendingOperationRepository pendingOperationRepository;
    private final ApprovalPersistenceService approvalPersistenceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final List<ApprovalHandler> handlers;

    private Map<String, ApprovalHandler> getRegistry() {
        return handlers.stream().collect(Collectors.toMap(ApprovalHandler::getEntityType, h -> h));
    }

    @Around("@annotation(requiresApproval)")
    public Object intercept(ProceedingJoinPoint pjp, RequiresApproval requiresApproval) throws Throwable {

        // Approve axınındayıqsa keç
        if (ApprovalContext.isApplying()) {
            return pjp.proceed();
        }

        Object[] args = pjp.getArgs();
        Long entityId = (Long) args[0];
        boolean isDelete = requiresApproval.isDelete();

        // Cari user-i al
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return pjp.proceed();
        }

        User performer = userRepository.findByIdAndDeletedFalse(principal.getId())
                .orElse(null);
        if (performer == null) {
            return pjp.proceed();
        }

        // Artıq pending əməliyyat varmı?
        if (pendingOperationRepository.existsByEntityTypeAndEntityIdAndStatusAndDeletedFalse(
                requiresApproval.entityType(), entityId, OperationStatus.PENDING)) {
            throw new BusinessException("Bu entity üçün artıq gözləyən əməliyyat mövcuddur");
        }

        // Handler tap
        ApprovalHandler handler = getRegistry().get(requiresApproval.entityType());
        if (handler == null) {
            log.warn("ApprovalHandler tapılmadı: {}", requiresApproval.entityType());
            return pjp.proceed();
        }

        // Old snapshot
        String oldJson = null;
        String label = null;
        try {
            Object snapshot = handler.getSnapshot(entityId);
            oldJson = objectMapper.writeValueAsString(snapshot);
            label = handler.getLabel(entityId);
        } catch (Exception e) {
            log.error("Old snapshot alınarkən xəta: {}", e.getMessage());
        }

        // New snapshot (yalnız EDIT üçün)
        String newJson = null;
        if (!isDelete && args.length > 1 && args[1] != null) {
            try {
                newJson = objectMapper.writeValueAsString(args[1]);
            } catch (Exception e) {
                log.error("New snapshot alınarkən xəta: {}", e.getMessage());
            }
        }

        Department dept = performer.getDepartment();

        PendingOperation op = PendingOperation.builder()
                .moduleCode(requiresApproval.module())
                .entityType(requiresApproval.entityType())
                .entityId(entityId)
                .entityLabel(label)
                .operationType(isDelete ? OperationType.DELETE : OperationType.EDIT)
                .performedBy(performer)
                .performerDepartment(dept)
                .oldSnapshot(oldJson)
                .newSnapshot(newJson)
                .build();

        // REQUIRES_NEW transaksiyada saxla — kənar rollback-dən qorunur
        PendingOperationResponse response = approvalPersistenceService.saveAndBuildResponse(op);
        throw new PendingApprovalException(response);
    }
}
