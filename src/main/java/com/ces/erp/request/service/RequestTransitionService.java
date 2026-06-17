package com.ces.erp.request.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.InvalidStatusTransitionException;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorğu (TechRequest) status keçidlərinin yeganə gateway-i.
 * <p>
 * Bütün servislər (TechRequest, ProjectManager, CoordinatorPlan, DocumentCheck) statusu
 * yalnız bu metod vasitəsilə dəyişir: validasiya + RequestStatusLog + audit bir yerdə.
 * Birbaşa {@code setStatus} çağırışları qadağandır.
 * <p>
 * Yalnız label əlavə olunan enum-lar kimi, bu da workflow state-machine-ə toxunmur —
 * sadəcə icazəli keçidləri (irəli + geri) mərkəzləşdirir.
 */
@Service
@RequiredArgsConstructor
public class RequestTransitionService {

    private final TechRequestRepository requestRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final AuditService auditService;

    /** İcazəli status keçidləri (irəli + REJECTED + 4 geri qaytarma). */
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS;
    static {
        Map<RequestStatus, Set<RequestStatus>> m = new EnumMap<>(RequestStatus.class);
        m.put(RequestStatus.DRAFT, Set.of(RequestStatus.PENDING, RequestStatus.REJECTED));
        m.put(RequestStatus.PENDING, Set.of(RequestStatus.PM_REVIEW, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_REVIEW, Set.of(RequestStatus.PM_SHORTLIST_READY, RequestStatus.REJECTED));
        m.put(RequestStatus.PM_SHORTLIST_READY, Set.of(RequestStatus.COORDINATOR_NEGOTIATING, RequestStatus.REJECTED));
        m.put(RequestStatus.COORDINATOR_NEGOTIATING, Set.of(RequestStatus.COORDINATOR_PROPOSED, RequestStatus.REJECTED));
        // Geri: koordinator öz təklifini geri alır
        m.put(RequestStatus.COORDINATOR_PROPOSED, Set.of(RequestStatus.PM_PRICE_NEGOTIATION, RequestStatus.REJECTED,
                RequestStatus.COORDINATOR_NEGOTIATING));
        // Geri: LM müştəri ilə razılaşa bilmədi, koordinatordan yeni təklif
        m.put(RequestStatus.PM_PRICE_NEGOTIATION, Set.of(RequestStatus.PM_APPROVED, RequestStatus.REJECTED,
                RequestStatus.COORDINATOR_NEGOTIATING));
        m.put(RequestStatus.PM_APPROVED, Set.of(RequestStatus.ACCOUNTING_DOCS_CHECK, RequestStatus.REJECTED));
        // Geri: maliyyə sənəd əskik/səhv, LM-ə iadə
        m.put(RequestStatus.ACCOUNTING_DOCS_CHECK, Set.of(RequestStatus.EXECUTION_READY, RequestStatus.REJECTED,
                RequestStatus.PM_PRICE_NEGOTIATION));
        // Geri: koordinator operatoru dəyişmək üçün geri qaytarır.
        // DELIVERED — çoxlu texnika modelində aqreqat: bütün xətlər təhvil verildikdə
        // sorğu birbaşa EXECUTION_READY → DELIVERED keçir (xətlər ayrıca icra olunur).
        m.put(RequestStatus.EXECUTION_READY, Set.of(RequestStatus.OPERATOR_ASSIGNED,
                RequestStatus.DELIVERED, RequestStatus.REJECTED));
        m.put(RequestStatus.OPERATOR_ASSIGNED, Set.of(RequestStatus.EQUIPMENT_DISPATCHED, RequestStatus.REJECTED,
                RequestStatus.EXECUTION_READY));
        // Texnika yola çıxandan sonra geri qaytarma YOXDUR
        m.put(RequestStatus.EQUIPMENT_DISPATCHED, Set.of(RequestStatus.DELIVERED, RequestStatus.REJECTED));
        m.put(RequestStatus.DELIVERED, Set.of());
        m.put(RequestStatus.REJECTED, Set.of());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(m);
    }

    /** Geri qaytarma keçidləri — bunlarda səbəb (reason) məcburidir. */
    private static final Set<Map.Entry<RequestStatus, RequestStatus>> SEND_BACK = Set.of(
            Map.entry(RequestStatus.PM_PRICE_NEGOTIATION, RequestStatus.COORDINATOR_NEGOTIATING),
            Map.entry(RequestStatus.COORDINATOR_PROPOSED, RequestStatus.COORDINATOR_NEGOTIATING),
            Map.entry(RequestStatus.ACCOUNTING_DOCS_CHECK, RequestStatus.PM_PRICE_NEGOTIATION),
            Map.entry(RequestStatus.OPERATOR_ASSIGNED, RequestStatus.EXECUTION_READY)
    );

    public boolean isSendBack(RequestStatus from, RequestStatus target) {
        return SEND_BACK.contains(Map.entry(from, target));
    }

    /**
     * Status keçidinin yeganə nöqtəsi: validasiya + status dəyişikliyi + RequestStatusLog + audit.
     *
     * @param request keçid ediləcək sorğu (status-u oxunur və yenilənir)
     * @param target  hədəf status
     * @param reason  səbəb — geri qaytarmalarda MƏCBURİ, irəli/REJECTED-də opsional
     * @param actor   əməliyyatı edən (null olduqda SecurityContext-dən götürülür)
     */
    @Transactional
    public TechRequest transition(TechRequest request, RequestStatus target, String reason, String actor) {
        RequestStatus from = request.getStatus();

        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStatusTransitionException(
                    from.name() + " statusundan " + target.name() + " statusuna keçid mümkün deyil");
        }

        if (isSendBack(from, target) && (reason == null || reason.isBlank())) {
            throw new BusinessException("Geri qaytarma üçün səbəb göstərilməlidir");
        }

        request.setStatus(target);
        TechRequest saved = requestRepository.save(request);

        String changedBy = (actor != null && !actor.isBlank()) ? actor : currentUsername();
        statusLogRepository.save(RequestStatusLog.builder()
                .requestId(saved.getId())
                .oldStatus(from)
                .newStatus(target)
                .reason(reason)
                .changedBy(changedBy)
                .build());

        auditService.log("SORĞU", saved.getId(), resolveCode(saved), "STATUS_DƏYİŞDİ",
                from.name() + " → " + target.name()
                        + (reason != null && !reason.isBlank() ? " | " + reason : ""));

        return saved;
    }

    /** Frontend üçün icazəli keçidlər xəritəsi (status-transitions endpoint-i). */
    public Map<String, List<String>> getAllowedTransitions() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<RequestStatus, Set<RequestStatus>> e : ALLOWED_TRANSITIONS.entrySet()) {
            result.put(e.getKey().name(), e.getValue().stream().map(Enum::name).toList());
        }
        return result;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String resolveCode(TechRequest entity) {
        return entity.getRequestCode() != null ? entity.getRequestCode()
                : "REQ-" + String.format("%04d", entity.getId());
    }
}
