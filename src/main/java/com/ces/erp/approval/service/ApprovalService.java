package com.ces.erp.approval.service;

import com.ces.erp.approval.dto.ApprovalSummaryResponse;
import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.dto.RejectRequest;
import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.department.entity.Department;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.ces.erp.role.entity.Role;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final PendingOperationRepository pendingOperationRepository;
    private final UserRepository userRepository;
    private final List<ApprovalHandler> handlers;
    private final AuditService auditService;

    private Map<String, ApprovalHandler> registry() {
        return handlers.stream().collect(Collectors.toMap(ApprovalHandler::getEntityType, h -> h));
    }

    public List<ApprovalSummaryResponse> getQueue(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        if (!hasApprovalAccess(user)) {
            return List.of();
        }

        List<Long> deptIds = getApprovalDeptIds(user);
        List<PendingOperation> ops;

        if (!deptIds.isEmpty()) {
            ops = pendingOperationRepository.findAllByDepartmentIds(deptIds);
        } else {
            // Heç bir şöbə verilməyib → hamısını göstər
            ops = pendingOperationRepository.findAllActive();
        }

        return ops.stream().map(ApprovalSummaryResponse::from).toList();
    }

    public PendingOperationResponse getDetail(Long id, Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        PendingOperation op = pendingOperationRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Əməliyyat", id));

        checkAccess(user, op);
        return PendingOperationResponse.from(op);
    }

    @Transactional
    public ApprovalSummaryResponse approve(Long id, Long userId) {
        User approver = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        if (!hasApprovalAccess(approver)) {
            throw new BusinessException("Sizin təsdiq icazəniz yoxdur");
        }

        PendingOperation op = pendingOperationRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Əməliyyat", id));

        if (op.getStatus() != OperationStatus.PENDING) {
            throw new BusinessException("Bu əməliyyat artıq emal edilib");
        }

        checkAccess(approver, op);

        ApprovalHandler handler = registry().get(op.getEntityType());
        if (handler == null) {
            throw new BusinessException("Bu entity tipi üçün handler tapılmadı: " + op.getEntityType());
        }

        if (op.getOperationType() == OperationType.EDIT) {
            handler.applyEdit(op.getEntityId(), op.getNewSnapshot());
        } else {
            handler.applyDelete(op.getEntityId());
        }

        op.setStatus(OperationStatus.APPROVED);
        op.setProcessedBy(approver);
        op.setProcessedAt(LocalDateTime.now());
        PendingOperation saved = pendingOperationRepository.save(op);
        auditService.log("TƏSDİQ", saved.getId(), saved.getEntityLabel(), "TƏSDİQLƏNDİ",
                saved.getModuleCode() + " — " + saved.getOperationType() + " əməliyyatı təsdiqləndi");
        return ApprovalSummaryResponse.from(saved);
    }

    @Transactional
    public ApprovalSummaryResponse reject(Long id, Long userId, RejectRequest request) {
        User approver = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        if (!hasApprovalAccess(approver)) {
            throw new BusinessException("Sizin təsdiq icazəniz yoxdur");
        }

        PendingOperation op = pendingOperationRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Əməliyyat", id));

        if (op.getStatus() != OperationStatus.PENDING) {
            throw new BusinessException("Bu əməliyyat artıq emal edilib");
        }

        checkAccess(approver, op);

        op.setStatus(OperationStatus.REJECTED);
        op.setProcessedBy(approver);
        op.setProcessedAt(LocalDateTime.now());
        op.setRejectReason(request != null ? request.getReason() : null);
        PendingOperation saved = pendingOperationRepository.save(op);
        auditService.log("TƏSDİQ", saved.getId(), saved.getEntityLabel(), "RƏDD EDİLDİ",
                saved.getModuleCode() + " — " + saved.getOperationType() + " əməliyyatı rədd edildi"
                + (saved.getRejectReason() != null ? ": " + saved.getRejectReason() : ""));
        return ApprovalSummaryResponse.from(saved);
    }

    private void checkAccess(User approver, PendingOperation op) {
        if (!hasApprovalAccess(approver)) {
            throw new BusinessException("Sizin təsdiq icazəniz yoxdur");
        }

        List<Long> allowedDepts = getApprovalDeptIds(approver);

        if (!allowedDepts.isEmpty() && op.getPerformerDepartment() != null) {
            if (!allowedDepts.contains(op.getPerformerDepartment().getId())) {
                throw new BusinessException("Bu şöbənin əməliyyatını təsdiq etmək icazəniz yoxdur");
            }
        }
    }

    /**
     * İstifadəçinin təsdiq icazəsi var mı — rolun OPERATIONS_APPROVAL icazəsindən
     * və ya köhnə user-səviyyə hasApproval bayrağından yoxlayır.
     */
    private boolean hasApprovalAccess(User user) {
        // Köhnə (user-səviyyə) yoxlama — geriyə uyğunluq
        if (user.isHasApproval()) return true;

        // Yeni (rol-səviyyə) yoxlama
        Role role = user.getRole();
        if (role == null || role.getPermissions() == null) return false;
        return role.getPermissions().stream()
                .anyMatch(p -> "OPERATIONS_APPROVAL".equals(p.getModule().getCode())
                        && (p.isCanGet() || p.isCanPut()));
    }

    /**
     * İstifadəçinin təsdiq edə biləcəyi şöbə ID-ləri —
     * əvvəlcə rolun approvalDepartments, sonra köhnə user-səviyyə yoxlanır.
     */
    private List<Long> getApprovalDeptIds(User user) {
        // Rolun approval şöbələri
        Role role = user.getRole();
        if (role != null && role.getApprovalDepartments() != null && !role.getApprovalDepartments().isEmpty()) {
            return role.getApprovalDepartments().stream()
                    .map(Department::getId)
                    .toList();
        }

        // Köhnə user-səviyyə (geriyə uyğunluq)
        if (user.getApprovalDepartments() != null && !user.getApprovalDepartments().isEmpty()) {
            return user.getApprovalDepartments().stream()
                    .map(ud -> ud.getDepartment().getId())
                    .toList();
        }

        return List.of();
    }
}
