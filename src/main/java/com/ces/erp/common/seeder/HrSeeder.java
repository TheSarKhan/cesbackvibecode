package com.ces.erp.common.seeder;

import com.ces.erp.hr.entity.Position;
import com.ces.erp.hr.entity.TaxRateConfig;
import com.ces.erp.hr.repository.PositionRepository;
import com.ces.erp.hr.repository.TaxRateConfigRepository;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.entity.RolePermission;
import com.ces.erp.role.repository.RolePermissionRepository;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.systemmodule.entity.SystemModule;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class HrSeeder implements CommandLineRunner {

    private final TaxRateConfigRepository taxRepo;
    private final PositionRepository positionRepo;
    private final SystemModuleRepository moduleRepo;
    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;

    @Override
    @Transactional
    public void run(String... args) {
        seedTaxRates();
        seedPositions();
        seedSuperAdminPermission();
    }

    private void seedTaxRates() {
        if (taxRepo.count() > 0) return;
        int year = LocalDate.now().getYear();
        log.info("HR: {} ili üçün default vergi tarifləri seed edilir...", year);

        TaxRateConfig cfg = TaxRateConfig.builder()
                .year(year)
                .active(true)
                // Pensiya — işçi
                .employeePensionThreshold(new BigDecimal("200.0000"))
                .employeePensionRateBelow(new BigDecimal("0.0300"))
                .employeePensionRateAbove(new BigDecimal("0.1000"))
                // Pensiya — işəgötürən
                .employerPensionThreshold(new BigDecimal("200.0000"))
                .employerPensionRateBelow(new BigDecimal("0.2200"))
                .employerPensionRateAbove(new BigDecimal("0.1500"))
                // İSH
                .employeeUnemploymentRate(new BigDecimal("0.0050"))
                .employerUnemploymentRate(new BigDecimal("0.0050"))
                // İTSH — işçi
                .employeeMedicalThreshold(new BigDecimal("8000.0000"))
                .employeeMedicalRateBelow(new BigDecimal("0.0200"))
                .employeeMedicalRateAbove(new BigDecimal("0.0050"))
                // İTSH — işəgötürən
                .employerMedicalThreshold(new BigDecimal("8000.0000"))
                .employerMedicalRateBelow(new BigDecimal("0.0200"))
                .employerMedicalRateAbove(new BigDecimal("0.0050"))
                // Gəlir vergisi
                .incomeTaxThreshold(new BigDecimal("8000.0000"))
                .incomeTaxRateBelow(new BigDecimal("0.0000"))
                .incomeTaxRateAbove(new BigDecimal("0.1400"))
                .nonTaxableMinimum(BigDecimal.ZERO)
                .deductSocialFromTaxBase(false)
                .notes("Qeyri-neft-qaz, qeyri-dövlət sektoru üçün default tarif")
                .build();
        taxRepo.save(cfg);
        log.info("Default vergi tarifi yaradıldı.");
    }

    private void seedPositions() {
        if (positionRepo.count() > 0) return;
        log.info("HR: default vəzifələr seed edilir...");
        positionRepo.saveAll(List.of(
                pos("Direktor", "Şirkət direktoru", new BigDecimal("9000.00")),
                pos("Baş Mühasib", "Maliyyə şöbəsi rəhbəri", new BigDecimal("3500.00")),
                pos("Mühasib", "Mühasib", new BigDecimal("2000.00")),
                pos("Xəzinədar", "Xəzinə işləri", new BigDecimal("1500.00")),
                pos("Sürücü", "Avtomobil sürücüsü", new BigDecimal("1000.00")),
                pos("Mühəndis", "Texniki mühəndis", new BigDecimal("2500.00")),
                pos("Operator", "Texnika operatoru", new BigDecimal("1800.00")),
                pos("Köməkçi işçi", "Köməkçi heyət", new BigDecimal("700.00"))
        ));
        log.info("8 default vəzifə əlavə edildi.");
    }

    private void seedSuperAdminPermission() {
        // Super admin-ə HR_MANAGEMENT-ə tam giriş ver (modul yenidirsə)
        SystemModule hrModule = moduleRepo.findAll().stream()
                .filter(m -> "HR_MANAGEMENT".equals(m.getCode()))
                .findFirst().orElse(null);
        if (hrModule == null) return;

        for (Role r : roleRepo.findAll()) {
            if (r.isDeleted()) continue;
            if (!"Super Admin".equalsIgnoreCase(r.getName())) continue;
            boolean alreadyHas = rolePermissionRepo.findAllByRoleId(r.getId()).stream()
                    .anyMatch(rp -> rp.getModule() != null && rp.getModule().getId().equals(hrModule.getId()));
            if (alreadyHas) continue;
            rolePermissionRepo.save(RolePermission.builder()
                    .role(r).module(hrModule)
                    .canGet(true).canPost(true).canPut(true).canDelete(true)
                    .canSendToCoordinator(false).canSubmitOffer(false)
                    .build());
            log.info("Super Admin rolu üçün HR_MANAGEMENT icazəsi əlavə edildi.");
        }
    }

    private Position pos(String name, String desc, BigDecimal salary) {
        return Position.builder()
                .name(name)
                .description(desc)
                .defaultSalary(salary)
                .active(true)
                .build();
    }
}
