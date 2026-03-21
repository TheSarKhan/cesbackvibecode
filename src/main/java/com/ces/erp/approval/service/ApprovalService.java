package com.ces.erp.approval.service;

import com.ces.erp.approval.dto.ApprovalSummaryResponse;
import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.dto.RejectRequest;
import com.ces.erp.approval.entity.PendingOperation;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.entity.UserApprovalDepartment;
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

    private Map<String, ApprovalHandler> registry() {
        return handlers.stream().collect(Collectors.toMap(ApprovalHandler::getEntityType, h -> h));
    }

    public List<ApprovalSummaryResponse> getQueue(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        List<PendingOperation> ops;

        if (user.isHasApproval() && !user.getApprovalDepartments().isEmpty()) {
            List<Long> deptIds = user.getApprovalDepartments().stream()
                    .map(ud -> ud.getDepartment().getId())
                    .toList();
            ops = pendingOperationRepository.findAllByDepartmentIds(deptIds);
        } else if (user.isHasApproval()) {
            // hasApproval amma heç bir şöbə verilməyib → hamısını göstər
            ops = pendingOperationRepository.findAllActive();
        } else {
            return List.of();
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

        if (!approver.isHasApproval()) {
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
        return ApprovalSummaryResponse.from(pendingOperationRepository.save(op));
    }

    @Transactional
    public ApprovalSummaryResponse reject(Long id, Long userId, RejectRequest request) {
        User approver = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", userId));

        if (!approver.isHasApproval()) {
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
        return ApprovalSummaryResponse.from(pendingOperationRepository.save(op));
    }

    private void checkAccess(User approver, PendingOperation op) {
        if (!approver.isHasApproval()) {
            throw new BusinessException("Sizin təsdiq icazəniz yoxdur");
        }
        // Şöbə yoxlaması: approvalDepartments boşdursa hamısına baxar
        List<Long> allowedDepts = approver.getApprovalDepartments().stream()
                .map(UserApprovalDepartment::getDepartment)
                .map(d -> d.getId())
                .toList();

        if (!allowedDepts.isEmpty() && op.getPerformerDepartment() != null) {
            if (!allowedDepts.contains(op.getPerformerDepartment().getId())) {
                throw new BusinessException("Bu şöbənin əməliyyatını təsdiq etmək icazəniz yoxdur");
            }
        }
    }
}
