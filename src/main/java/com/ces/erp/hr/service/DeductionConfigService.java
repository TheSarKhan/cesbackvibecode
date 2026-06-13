package com.ces.erp.hr.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.DeductionAppliesTo;
import com.ces.erp.enums.DeductionParty;
import com.ces.erp.hr.dto.*;
import com.ces.erp.hr.entity.DeductionBracket;
import com.ces.erp.hr.entity.DeductionConfigVersion;
import com.ces.erp.hr.entity.DeductionType;
import com.ces.erp.hr.repository.DeductionBracketRepository;
import com.ces.erp.hr.repository.DeductionConfigVersionRepository;
import com.ces.erp.hr.repository.DeductionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generic tutulma konfiqurasiyasının idarəetməsi — tutulma növləri, versiyalar, aralıqlar,
 * canlı önizləmə və tarixə görə həll (resolve).
 */
@Service
@RequiredArgsConstructor
public class DeductionConfigService {

    private static final String AUDIT_MODULE = "HR_TUTULMA_KONFIQ";

    private final DeductionTypeRepository typeRepo;
    private final DeductionConfigVersionRepository versionRepo;
    private final DeductionBracketRepository bracketRepo;
    private final DeductionCalculator calculator;
    private final AuditService auditService;

    // ─────────────────────────── Tutulma növləri (type) CRUD ───────────────────────────

    public List<DeductionTypeDto> getTypes() {
        return typeRepo.findAllByDeletedFalseOrderByDisplayOrderAscIdAsc().stream()
                .map(DeductionTypeDto::from).toList();
    }

    @Transactional
    public DeductionTypeDto createType(DeductionTypeDto dto) {
        if (dto.code() == null || dto.code().isBlank()) {
            throw new BusinessException("Tutulma növü kodu boş ola bilməz");
        }
        if (dto.appliesTo() == null) {
            throw new BusinessException("Tərəf (applies_to) seçilməlidir");
        }
        if (typeRepo.existsByCodeIgnoreCaseAndDeletedFalse(dto.code().trim())) {
            throw new DuplicateResourceException("Bu kod ilə tutulma növü artıq mövcuddur: " + dto.code());
        }
        DeductionType t = DeductionType.builder()
                .code(dto.code().trim().toUpperCase())
                .name(dto.name())
                .appliesTo(dto.appliesTo())
                .deductedFromNet(dto.deductedFromNet() == null || dto.deductedFromNet())
                .displayOrder(dto.displayOrder() == null ? 0 : dto.displayOrder())
                .active(dto.active() == null || dto.active())
                .build();
        DeductionType saved = typeRepo.save(t);
        auditService.log(AUDIT_MODULE, saved.getId(), saved.getCode(), "YARADILDI", "Tutulma növü yaradıldı");
        return DeductionTypeDto.from(saved);
    }

    @Transactional
    public DeductionTypeDto updateType(Long id, DeductionTypeDto dto) {
        DeductionType t = typeRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutulma növü", id));
        if (dto.name() != null) t.setName(dto.name());
        if (dto.appliesTo() != null) t.setAppliesTo(dto.appliesTo());
        if (dto.deductedFromNet() != null) t.setDeductedFromNet(dto.deductedFromNet());
        if (dto.displayOrder() != null) t.setDisplayOrder(dto.displayOrder());
        if (dto.active() != null) t.setActive(dto.active());
        DeductionType saved = typeRepo.save(t);
        auditService.log(AUDIT_MODULE, saved.getId(), saved.getCode(), "YENİLƏNDİ", "Tutulma növü yeniləndi");
        return DeductionTypeDto.from(saved);
    }

    @Transactional
    public void deleteType(Long id) {
        DeductionType t = typeRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutulma növü", id));
        t.softDelete();
        typeRepo.save(t);
        auditService.log(AUDIT_MODULE, t.getId(), t.getCode(), "SİLİNDİ", "Tutulma növü silindi");
    }

    // ─────────────────────────── Versiyalar ───────────────────────────

    public List<DeductionConfigVersionResponse> getVersions() {
        return versionRepo.findAllByDeletedFalseOrderByEffectiveDateDescVersionNoDesc().stream()
                .map(v -> toVersionResponse(v, false))
                .toList();
    }

    public DeductionConfigVersionResponse getVersion(Long id) {
        DeductionConfigVersion v = versionRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versiya", id));
        return toVersionResponse(v, true);
    }

    public DeductionConfigVersionResponse getActive() {
        DeductionConfigVersion v = versionRepo.findFirstByActiveTrueAndDeletedFalseOrderByEffectiveDateDescVersionNoDesc()
                .orElseThrow(() -> new BusinessException(
                        "Aktiv tutulma konfiqurasiyası tapılmadı. Konfiqurasiya səhifəsindən versiya yaradın."));
        return toVersionResponse(v, true);
    }

    @Transactional
    public DeductionConfigVersionResponse createVersion(CreateVersionRequest req, String createdBy) {
        int nextNo = versionRepo.findFirstByOrderByVersionNoDesc()
                .map(v -> v.getVersionNo() + 1).orElse(1);

        DeductionConfigVersion version = DeductionConfigVersion.builder()
                .versionNo(nextNo)
                .effectiveDate(req.effectiveDate())
                .active(false)
                .createdBy(createdBy)
                .note(req.note())
                .build();
        DeductionConfigVersion savedVersion = versionRepo.save(version);

        // Aralıqları yarat — hər qrup mövcud tutulma növünə istinad edir
        if (req.groups() != null) {
            for (DeductionGroupDto g : req.groups()) {
                if (g.deductionTypeId() == null) {
                    throw new BusinessException("Hər qrup üçün tutulma növü (deductionTypeId) tələb olunur");
                }
                DeductionType type = typeRepo.findByIdAndDeletedFalse(g.deductionTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tutulma növü", g.deductionTypeId()));

                saveBrackets(savedVersion, type, DeductionParty.ISCI, g.isciBrackets());
                saveBrackets(savedVersion, type, DeductionParty.ISEGOTUREN, g.isegoturenBrackets());
            }
        }

        if (Boolean.TRUE.equals(req.active())) {
            deactivateAllOthers(savedVersion.getId());
            savedVersion.setActive(true);
            versionRepo.save(savedVersion);
        }

        auditService.log(AUDIT_MODULE, savedVersion.getId(), "v" + nextNo, "YARADILDI",
                "Yeni konfiqurasiya versiyası yaradıldı");
        return toVersionResponse(savedVersion, true);
    }

    @Transactional
    public DeductionConfigVersionResponse activate(Long id) {
        DeductionConfigVersion v = versionRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versiya", id));
        deactivateAllOthers(id);
        v.setActive(true);
        versionRepo.save(v);
        auditService.log(AUDIT_MODULE, v.getId(), "v" + v.getVersionNo(), "AKTİVLƏŞDİRİLDİ", "Versiya aktivləşdirildi");
        return toVersionResponse(v, true);
    }

    @Transactional
    public void deleteVersion(Long id) {
        DeductionConfigVersion v = versionRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versiya", id));
        if (v.isActive()) {
            throw new BusinessException("Aktiv versiyanı silmək olmaz. Əvvəlcə başqa versiyanı aktivləşdirin.");
        }
        for (DeductionBracket b : bracketRepo.findAllByVersionIdAndDeletedFalseOrderBySortOrderAsc(id)) {
            b.softDelete();
            bracketRepo.save(b);
        }
        v.softDelete();
        versionRepo.save(v);
        auditService.log(AUDIT_MODULE, v.getId(), "v" + v.getVersionNo(), "SİLİNDİ", "Versiya silindi");
    }

    // ─────────────────────────── Canlı önizləmə ───────────────────────────

    public DeductionPreviewResponse preview(DeductionPreviewRequest req) {
        List<DeductionCalculator.DeductionDef> defs = new ArrayList<>();
        if (req.groups() != null) {
            for (DeductionGroupDto g : req.groups()) {
                DeductionAppliesTo appliesTo = g.appliesTo() != null ? g.appliesTo() : DeductionAppliesTo.HER_IKISI;
                defs.add(new DeductionCalculator.DeductionDef(
                        g.code(),
                        g.name(),
                        appliesTo,
                        g.deductedFromNet() == null || g.deductedFromNet(),
                        toBracketDefs(g.isciBrackets()),
                        toBracketDefs(g.isegoturenBrackets())));
            }
        }
        DeductionCalculator.Result r = calculator.compute(req.base(), defs);
        List<DeductionPreviewResponse.Line> lines = r.lines().stream()
                .map(l -> new DeductionPreviewResponse.Line(l.code(), l.name(), l.deductedFromNet(),
                        l.employeeAmount(), l.employerAmount()))
                .toList();
        return new DeductionPreviewResponse(r.base(), lines,
                r.totalEmployeeDeductions(), r.totalEmployerContributions(), r.netPay());
    }

    // ─────────────────────────── Tarixə görə həll (resolve) — PayrollService üçün ───────────────────────────

    /**
     * Verilən tarix üçün qüvvədə olan konfiqurasiyanı qaytarır.
     * Seçim: effective_date ≤ tarix olan ən son versiya; tapılmazsa aktiv versiya.
     */
    public ResolvedDeductionConfig resolveForDate(LocalDate date) {
        DeductionConfigVersion v = versionRepo
                .findFirstByDeletedFalseAndEffectiveDateLessThanEqualOrderByEffectiveDateDescVersionNoDesc(date)
                .or(versionRepo::findFirstByActiveTrueAndDeletedFalseOrderByEffectiveDateDescVersionNoDesc)
                .orElseThrow(() -> new BusinessException(
                        date + " tarixi üçün tutulma konfiqurasiyası tapılmadı."));
        return new ResolvedDeductionConfig(v.getId(), v.getVersionNo(), v.getEffectiveDate(), buildDefs(v.getId()));
    }

    // ─────────────────────────── köməkçilər ───────────────────────────

    private void saveBrackets(DeductionConfigVersion version, DeductionType type,
                              DeductionParty party, List<BracketDto> brackets) {
        if (brackets == null || brackets.isEmpty()) return;

        // Validasiya: applies_to ↔ party uyğunluğu
        if (!type.getAppliesTo().allows(party)) {
            throw new BusinessException(String.format(
                    "'%s' tutulma növü yalnız %s tərəfinə aiddir; %s aralığı əlavə edilə bilməz.",
                    type.getName(), partyLabel(type.getAppliesTo()), partyLabel(party)));
        }

        List<BracketDto> sorted = new ArrayList<>(brackets);
        sorted.sort(Comparator.comparing(b -> nz(b.lowerBound())));
        validateContiguous(sorted, type.getName(), party);

        int order = 0;
        for (BracketDto b : sorted) {
            DeductionBracket entity = DeductionBracket.builder()
                    .version(version)
                    .deductionType(type)
                    .party(party)
                    .lowerBound(nz(b.lowerBound()))
                    .upperBound(b.upperBound())
                    .fixedAmount(nz(b.fixedAmount()))
                    .rate(nz(b.rate()))
                    .sortOrder(b.sortOrder() != null ? b.sortOrder() : order++)
                    .build();
            bracketRepo.save(entity);
        }
    }

    /** Aralıqların ardıcıllığını yoxlayır: overlap yox, boşluq yox, faiz 0–100%, alt &lt; üst. */
    private void validateContiguous(List<BracketDto> sorted, String typeName, DeductionParty party) {
        BigDecimal prevUpper = null;
        for (int i = 0; i < sorted.size(); i++) {
            BracketDto b = sorted.get(i);
            BigDecimal lower = nz(b.lowerBound());
            BigDecimal upper = b.upperBound();
            BigDecimal rate = nz(b.rate());

            if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(typeName + ": faiz 0–100% aralığında olmalıdır");
            }
            if (lower.signum() < 0) {
                throw new BusinessException(typeName + ": alt hədd mənfi ola bilməz");
            }
            if (upper != null && upper.compareTo(lower) <= 0) {
                throw new BusinessException(typeName + ": alt hədd üst həddən kiçik olmalıdır");
            }
            // boşluq / overlap: hər aralığın alt həddi əvvəlkinin üst həddinə bərabər olmalıdır
            if (prevUpper == null) {
                if (i > 0) {
                    throw new BusinessException(typeName + ": yalnız sonuncu aralıq sonsuz (üst hədd boş) ola bilər");
                }
            } else if (lower.compareTo(prevUpper) != 0) {
                throw new BusinessException(typeName + ": aralıqlar arasında boşluq və ya üst-üstə düşmə var");
            }
            prevUpper = upper;
        }
    }

    private List<DeductionCalculator.BracketDef> toBracketDefs(List<BracketDto> brackets) {
        if (brackets == null) return List.of();
        return brackets.stream()
                .sorted(Comparator.comparing(b -> nz(b.lowerBound())))
                .map(b -> new DeductionCalculator.BracketDef(
                        nz(b.lowerBound()), b.upperBound(), nz(b.fixedAmount()), nz(b.rate())))
                .toList();
    }

    /** DB-dəki bir versiyanın aralıqlarından motor üçün DeductionDef-lər qurur. */
    private List<DeductionCalculator.DeductionDef> buildDefs(Long versionId) {
        List<DeductionBracket> all = bracketRepo.findByVersionWithType(versionId);
        // növə görə qrupla
        List<DeductionType> types = all.stream()
                .map(DeductionBracket::getDeductionType)
                .filter(DeductionType::isActive)
                .distinct()
                .sorted(Comparator.comparing(DeductionType::getDisplayOrder).thenComparing(DeductionType::getId))
                .toList();

        List<DeductionCalculator.DeductionDef> defs = new ArrayList<>();
        for (DeductionType t : types) {
            List<DeductionCalculator.BracketDef> isci = bracketDefsForParty(all, t, DeductionParty.ISCI);
            List<DeductionCalculator.BracketDef> iseg = bracketDefsForParty(all, t, DeductionParty.ISEGOTUREN);
            defs.add(new DeductionCalculator.DeductionDef(
                    t.getCode(), t.getName(), t.getAppliesTo(), t.isDeductedFromNet(), isci, iseg));
        }
        return defs;
    }

    private List<DeductionCalculator.BracketDef> bracketDefsForParty(List<DeductionBracket> all,
                                                                     DeductionType type, DeductionParty party) {
        return all.stream()
                .filter(b -> b.getDeductionType().getId().equals(type.getId()) && b.getParty() == party)
                .sorted(Comparator.comparing(DeductionBracket::getSortOrder)
                        .thenComparing(b -> nz(b.getLowerBound())))
                .map(b -> new DeductionCalculator.BracketDef(
                        nz(b.getLowerBound()), b.getUpperBound(), nz(b.getFixedAmount()), nz(b.getRate())))
                .toList();
    }

    private DeductionConfigVersionResponse toVersionResponse(DeductionConfigVersion v, boolean withGroups) {
        List<DeductionGroupDto> groups = null;
        if (withGroups) {
            groups = buildGroups(v.getId());
        }
        return new DeductionConfigVersionResponse(v.getId(), v.getVersionNo(), v.getEffectiveDate(),
                v.isActive(), v.getCreatedBy(), v.getNote(), v.getCreatedAt(), groups);
    }

    private List<DeductionGroupDto> buildGroups(Long versionId) {
        List<DeductionBracket> all = bracketRepo.findByVersionWithType(versionId);
        List<DeductionType> types = all.stream()
                .map(DeductionBracket::getDeductionType)
                .distinct()
                .sorted(Comparator.comparing(DeductionType::getDisplayOrder).thenComparing(DeductionType::getId))
                .toList();

        List<DeductionGroupDto> groups = new ArrayList<>();
        for (DeductionType t : types) {
            groups.add(new DeductionGroupDto(
                    t.getId(), t.getCode(), t.getName(), t.getAppliesTo(), t.isDeductedFromNet(),
                    t.getDisplayOrder(), t.isActive(),
                    bracketDtosForParty(all, t, DeductionParty.ISCI),
                    bracketDtosForParty(all, t, DeductionParty.ISEGOTUREN)));
        }
        return groups;
    }

    private List<BracketDto> bracketDtosForParty(List<DeductionBracket> all, DeductionType type, DeductionParty party) {
        return all.stream()
                .filter(b -> b.getDeductionType().getId().equals(type.getId()) && b.getParty() == party)
                .sorted(Comparator.comparing(DeductionBracket::getSortOrder)
                        .thenComparing(b -> nz(b.getLowerBound())))
                .map(b -> new BracketDto(b.getId(), b.getLowerBound(), b.getUpperBound(),
                        b.getFixedAmount(), b.getRate(), b.getSortOrder()))
                .toList();
    }

    private void deactivateAllOthers(Long keepId) {
        versionRepo.findAllByDeletedFalseOrderByEffectiveDateDescVersionNoDesc().forEach(v -> {
            if ((keepId == null || !v.getId().equals(keepId)) && v.isActive()) {
                v.setActive(false);
                versionRepo.save(v);
            }
        });
    }

    private static String partyLabel(DeductionAppliesTo a) {
        return switch (a) {
            case ISCI -> "İŞÇİ";
            case ISEGOTUREN -> "İŞƏGÖTÜRƏN";
            case HER_IKISI -> "HƏR İKİ";
        };
    }

    private static String partyLabel(DeductionParty p) {
        return p == DeductionParty.ISCI ? "İŞÇİ" : "İŞƏGÖTÜRƏN";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
