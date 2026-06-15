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
import com.ces.erp.common.websocket.NotificationService;
import com.ces.erp.department.entity.Department;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.OperationType;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ApprovalAspect {

    private final PendingOperationRepository pendingOperationRepository;
    private final ApprovalPersistenceService approvalPersistenceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final List<ApprovalHandler> handlers;
    private final NotificationService notificationService;

    private Map<String, ApprovalHandler> getRegistry() {
        return handlers.stream().collect(Collectors.toMap(ApprovalHandler::getEntityType, h -> h));
    }

    /**
     * Edit-də əslində dəyişiklik baş veribmi yoxlayır. Request DTO-nun null olmayan hər sahəsini
     * old snapshot-da müvafiq sahə ilə müqayisə edir. Sahə adları üst-üstə düşürsə
     * (məs request.firstName ↔ snapshot.firstName), sadə dəyər müqayisəsi olur.
     * `xxxId` formatlı sahə üçün snapshot-da `xxx.id` nested forması da yoxlanır
     * (məs request.positionId ↔ snapshot.position.id). Tapılmayan sahələr
     * "dəyişiklik var" hesab olunur (ehtiyatlı default).
     */
    private boolean hasMeaningfulChange(String oldJson, Object newRequest) {
        if (oldJson == null || newRequest == null) return true;
        try {
            JsonNode oldNode = objectMapper.readTree(oldJson);
            JsonNode reqNode = objectMapper.valueToTree(newRequest);
            if (!reqNode.isObject() || !oldNode.isObject()) return true;

            Iterator<Map.Entry<String, JsonNode>> fields = reqNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String field = entry.getKey();
                JsonNode reqVal = entry.getValue();

                // null/missing request dəyəri — istifadəçi bu sahəni dəyişmək niyyətində deyil
                if (reqVal == null || reqVal.isNull()) continue;

                JsonNode oldVal = oldNode.get(field);

                // xxxId → snapshot.xxx.id (nested obyekt) yanaşması
                if ((oldVal == null || oldVal.isNull()) && field.endsWith("Id") && field.length() > 2) {
                    String nestedName = field.substring(0, field.length() - 2);
                    JsonNode nested = oldNode.get(nestedName);
                    if (nested != null && nested.isObject() && nested.has("id")) {
                        oldVal = nested.get("id");
                    }
                }

                if (oldVal == null || oldVal.isNull()) {
                    // request-də dəyər, snapshot-da yoxdur — dəyişiklikdir
                    return true;
                }

                if (!sameValue(reqVal, oldVal)) {
                    return true;
                }
            }
            return false; // bütün request sahələri snapshot ilə üst-üstə düşür
        } catch (Exception e) {
            log.warn("hasMeaningfulChange müqayisə xətası: {}", e.getMessage());
            return true; // ehtiyatlı: müqayisə uğursuz olarsa təsdiq növbəsinə sal
        }
    }

    private boolean sameValue(JsonNode a, JsonNode b) {
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue()) == 0;
        }
        if (a.isValueNode() && b.isValueNode()) {
            return Objects.equals(a.asText(), b.asText());
        }
        // mürəkkəb tip — JSON struktur müqayisəsi
        return a.equals(b);
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

        // DELETE üçün mövcud PENDING op-a smart davranış:
        //   • Eyni entity üçün PENDING DELETE varsa → idempotent: mövcud op-u qaytar
        //   • Eyni entity üçün PENDING EDIT varsa → EDIT-i avtomatik rədd et, sonra yeni DELETE-i növbəyə qoy
        // EDIT üçün məhdudiyyət yoxdur — istənilən vaxt yeni edit növbəyə əlavə oluna bilər.
        if (isDelete) {
            var existingOpt = approvalPersistenceService
                    .findExistingPending(requiresApproval.entityType(), entityId);
            if (existingOpt.isPresent()) {
                PendingOperationResponse existing = existingOpt.get();
                if (existing.getOperationType() == OperationType.DELETE) {
                    // Eyni delete istəyi artıq növbədədir — idempotent qaytar
                    throw new PendingApprovalException(existing);
                }
                // EDIT pending varsa onu avtomatik rədd et (delete daha güclüdür)
                approvalPersistenceService.autoRejectExistingOperation(
                        existing.getId(), performer, "Silmə əməliyyatı ilə əvəzləndi");
                log.debug("Edit pending op #{} avtomatik rədd edildi (delete üzərində yazdı)", existing.getId());
            }
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

        // New snapshot (yalnız EDIT üçün):
        //   newJson  → request DTO (applyEdit BUNU işlədir — dəyişdirmək olmaz!)
        //   viewJson → oxunaqlı "sonrakı" snapshot (getSnapshot formasında) — yalnız diff göstərimi üçün
        String newJson = null;
        String viewJson = null;
        if (!isDelete && args.length > 1 && args[1] != null) {
            try {
                newJson = objectMapper.writeValueAsString(args[1]);
            } catch (Exception e) {
                log.error("New snapshot alınarkən xəta: {}", e.getMessage());
            }
            try {
                Object after = handler.getAfterSnapshot(entityId, args[1]);
                if (after != null) viewJson = objectMapper.writeValueAsString(after);
            } catch (Exception e) {
                log.error("New snapshot view alınarkən xəta: {}", e.getMessage());
            }

            // Edit halında dəyişiklik yoxdursa — təsdiq növbəsinə salma, proceed et
            if (!hasMeaningfulChange(oldJson, args[1])) {
                log.debug("Edit-də heç bir dəyişiklik tapılmadı, təsdiq atılmır: {} #{}",
                        requiresApproval.entityType(), entityId);
                return pjp.proceed();
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
                .newSnapshotView(viewJson)
                .build();

        // REQUIRES_NEW transaksiyada saxla — kənar rollback-dən qorunur
        PendingOperationResponse response = approvalPersistenceService.saveAndBuildResponse(op);
        notificationService.approvalQueueUpdated(
                (label != null ? label : requiresApproval.entityType()) + " əməliyyatı təsdiq növbəsinə əlavə edildi");
        throw new PendingApprovalException(response);
    }
}
