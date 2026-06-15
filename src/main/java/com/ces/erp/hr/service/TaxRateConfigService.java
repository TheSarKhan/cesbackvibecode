package com.ces.erp.hr.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.hr.dto.TaxRateConfigRequest;
import com.ces.erp.hr.dto.TaxRateConfigResponse;
import com.ces.erp.hr.entity.TaxRateConfig;
import com.ces.erp.hr.repository.TaxRateConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxRateConfigService implements ApprovalHandler {

    private final TaxRateConfigRepository repo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "TAX_RATE"; }
    @Override public String getModuleCode()  { return "HR"; }
    @Override public String getLabel(Long id) { return String.valueOf(loadActive(id).getYear()) + " vergi tarifi"; }
    @Override public Object getSnapshot(Long id) { return TaxRateConfigResponse.from(loadActive(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            TaxRateConfigRequest req = objectMapper.readValue(json, TaxRateConfigRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<TaxRateConfigResponse> getAll() {
        return repo.findAllByDeletedFalseOrderByYearDesc().stream()
                .map(TaxRateConfigResponse::from)
                .toList();
    }

    public TaxRateConfigResponse getById(Long id) {
        return TaxRateConfigResponse.from(loadActive(id));
    }

    public TaxRateConfigResponse getActive() {
        TaxRateConfig active = resolveActive();
        return TaxRateConfigResponse.from(active);
    }

    public TaxRateConfig resolveActive() {
        return repo.findFirstByActiveTrueAndDeletedFalseOrderByYearDesc()
                .orElseGet(() -> repo.findByYearAndDeletedFalse(LocalDate.now().getYear())
                        .orElseThrow(() -> new BusinessException(
                                "Aktiv vergi tarifi tapılmadı. Konfiqurasiya səhifəsindən tarif yaradın.")));
    }

    @Transactional
    public TaxRateConfigResponse create(TaxRateConfigRequest req) {
        if (repo.findByYearAndDeletedFalse(req.getYear()).isPresent()) {
            throw new DuplicateResourceException("Bu il üçün artıq tarif mövcuddur");
        }
        TaxRateConfig c = new TaxRateConfig();
        c.setYear(req.getYear());
        applyRequest(c, req);
        if (Boolean.TRUE.equals(req.getActive())) {
            deactivateAllOthers(null);
            c.setActive(true);
        } else {
            c.setActive(req.getActive() == null ? false : req.getActive());
        }
        TaxRateConfig saved = repo.save(c);
        auditService.log("HR_VERGİ_TARİFİ", saved.getId(), String.valueOf(saved.getYear()), "YARADILDI",
                "İl üzrə vergi tarifi yaradıldı");
        return TaxRateConfigResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "TAX_RATE")
    public TaxRateConfigResponse update(Long id, TaxRateConfigRequest req) {
        TaxRateConfig c = loadActive(id);
        if (req.getYear() != null && !req.getYear().equals(c.getYear())) {
            if (repo.findByYearAndDeletedFalse(req.getYear()).isPresent()) {
                throw new DuplicateResourceException("Bu il üçün artıq tarif mövcuddur");
            }
            c.setYear(req.getYear());
        }
        applyRequest(c, req);
        if (Boolean.TRUE.equals(req.getActive())) {
            deactivateAllOthers(c.getId());
            c.setActive(true);
        } else if (req.getActive() != null) {
            c.setActive(req.getActive());
        }
        TaxRateConfig saved = repo.save(c);
        auditService.log("HR_VERGİ_TARİFİ", saved.getId(), String.valueOf(saved.getYear()), "YENİLƏNDİ",
                "Vergi tarifi yeniləndi");
        return TaxRateConfigResponse.from(saved);
    }

    @Transactional
    public TaxRateConfigResponse activate(Long id) {
        TaxRateConfig c = loadActive(id);
        deactivateAllOthers(id);
        c.setActive(true);
        TaxRateConfig saved = repo.save(c);
        auditService.log("HR_VERGİ_TARİFİ", saved.getId(), String.valueOf(saved.getYear()), "AKTİVLƏŞDİRİLDİ",
                "İllik tarif aktiv edildi");
        return TaxRateConfigResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "TAX_RATE", isDelete = true)
    public void delete(Long id) {
        TaxRateConfig c = loadActive(id);
        if (c.isActive()) {
            throw new BusinessException("Aktiv tarifi silmək olmaz. Əvvəlcə başqa bir tarifi aktivləşdirin.");
        }
        c.softDelete();
        repo.save(c);
        auditService.log("HR_VERGİ_TARİFİ", c.getId(), String.valueOf(c.getYear()), "SİLİNDİ", "Tarif silindi");
    }

    // ─── köməkçilər ──
    private TaxRateConfig loadActive(Long id) {
        return repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vergi tarifi", id));
    }

    private void deactivateAllOthers(Long keepId) {
        repo.findAllByDeletedFalseOrderByYearDesc().forEach(c -> {
            if (keepId == null || !c.getId().equals(keepId)) {
                if (c.isActive()) {
                    c.setActive(false);
                    repo.save(c);
                }
            }
        });
    }

    private void applyRequest(TaxRateConfig c, TaxRateConfigRequest r) {
        if (r.getEmployeePensionThreshold() != null) c.setEmployeePensionThreshold(r.getEmployeePensionThreshold());
        if (r.getEmployeePensionRateBelow() != null) c.setEmployeePensionRateBelow(r.getEmployeePensionRateBelow());
        if (r.getEmployeePensionRateAbove() != null) c.setEmployeePensionRateAbove(r.getEmployeePensionRateAbove());

        if (r.getEmployerPensionThreshold() != null) c.setEmployerPensionThreshold(r.getEmployerPensionThreshold());
        if (r.getEmployerPensionRateBelow() != null) c.setEmployerPensionRateBelow(r.getEmployerPensionRateBelow());
        if (r.getEmployerPensionRateAbove() != null) c.setEmployerPensionRateAbove(r.getEmployerPensionRateAbove());

        if (r.getEmployeeUnemploymentRate() != null) c.setEmployeeUnemploymentRate(r.getEmployeeUnemploymentRate());
        if (r.getEmployerUnemploymentRate() != null) c.setEmployerUnemploymentRate(r.getEmployerUnemploymentRate());

        if (r.getEmployeeMedicalThreshold() != null) c.setEmployeeMedicalThreshold(r.getEmployeeMedicalThreshold());
        if (r.getEmployeeMedicalRateBelow() != null) c.setEmployeeMedicalRateBelow(r.getEmployeeMedicalRateBelow());
        if (r.getEmployeeMedicalRateAbove() != null) c.setEmployeeMedicalRateAbove(r.getEmployeeMedicalRateAbove());

        if (r.getEmployerMedicalThreshold() != null) c.setEmployerMedicalThreshold(r.getEmployerMedicalThreshold());
        if (r.getEmployerMedicalRateBelow() != null) c.setEmployerMedicalRateBelow(r.getEmployerMedicalRateBelow());
        if (r.getEmployerMedicalRateAbove() != null) c.setEmployerMedicalRateAbove(r.getEmployerMedicalRateAbove());

        if (r.getIncomeTaxThreshold() != null) c.setIncomeTaxThreshold(r.getIncomeTaxThreshold());
        if (r.getIncomeTaxRateBelow() != null) c.setIncomeTaxRateBelow(r.getIncomeTaxRateBelow());
        if (r.getIncomeTaxRateAbove() != null) c.setIncomeTaxRateAbove(r.getIncomeTaxRateAbove());

        if (r.getNonTaxableMinimum() != null) c.setNonTaxableMinimum(r.getNonTaxableMinimum());
        if (r.getDeductSocialFromTaxBase() != null) c.setDeductSocialFromTaxBase(r.getDeductSocialFromTaxBase());
        if (r.getNotes() != null) c.setNotes(r.getNotes());
    }
}
