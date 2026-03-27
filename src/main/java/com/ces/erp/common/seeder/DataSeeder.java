package com.ces.erp.common.seeder;

import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.entity.RolePermission;
import com.ces.erp.role.repository.RolePermissionRepository;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.systemmodule.entity.SystemModule;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SystemModuleRepository moduleRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (moduleRepository.count() == 0) {
            log.info("Modullar seed edilir...");
            seedModules();
        }
        if (!userRepository.existsByEmailAndDeletedFalse("admin@ces.az")) {
            log.info("Rollar və istifadəçilər seed edilir...");
            seedDepartmentsRolesUsers();
            log.info("Sistem seed tamamlandı.");
        }
    }

    // ─── Sistem modulları ─────────────────────────────────────────────────────

    private void seedModules() {
        List<SystemModule> modules = List.of(
                module("CUSTOMER_MANAGEMENT",   "Müştəri İdarəetməsi",        1),
                module("CONTRACTOR_MANAGEMENT", "Podratçı İdarəetməsi",       2),
                module("ROLE_PERMISSION",        "Rol və İcazə İdarəetməsi",   3),
                module("EMPLOYEE_MANAGEMENT",    "İşçi İdarəetməsi",           4),
                module("GARAGE",                 "Qaraj Modulu",               5),
                module("REQUESTS",               "Sorğular Modulu",            6),
                module("COORDINATOR",            "Koordinator Modulu",         7),
                module("PROJECTS",               "Layihələr Modulu",           8),
                module("ACCOUNTING",             "Mühasibatlıq Modulu",        9),
                module("SERVICE_MANAGEMENT",     "Texniki Servis Modulu",     10),
                module("INVESTORS",              "İnvestor İdarəetməsi",      11),
                module("OPERATORS",              "Operator İdarəetməsi",      12),
                module("OPERATIONS_APPROVAL",    "Əməliyyatların Təsdiqi",    13),
                module("TRASH",                  "Silinmiş Məlumatlar",       14),
                module("AUDIT_LOG",              "Audit Jurnal",              15),
                module("CONFIG",                 "Konfiqurasiya Modulu",      16)
        );
        moduleRepository.saveAll(modules);
        log.info("{} modul əlavə edildi.", modules.size());
    }

    // ─── Şöbələr, rollar, istifadəçilər ──────────────────────────────────────

    private void seedDepartmentsRolesUsers() {
        List<SystemModule> allModules = moduleRepository.findAll();
        Map<String, SystemModule> byCode = new java.util.HashMap<>();
        allModules.forEach(m -> byCode.put(m.getCode(), m));

        // ── Şöbələr ──
        Department rehberlik  = dept("Rəhbərlik",            "Şirkət rəhbərliyi");
        Department satis      = dept("Satış şöbəsi",         "Müştəri və sorğu idarəetməsi");
        Department koord      = dept("Koordinasiya şöbəsi",  "Koordinator əməliyyatları");
        Department maliyye    = dept("Maliyyə şöbəsi",       "Mühasibatlıq və maliyyə");
        Department texniki    = dept("Texniki Xidmət şöbəsi","Qaraj və avadanlıq idarəetməsi");
        departmentRepository.saveAll(List.of(rehberlik, satis, koord, maliyye, texniki));

        // ── Rollar ──

        // 1. Super Admin — hər şeyə tam giriş
        Role superAdmin = role("Super Admin", "Bütün modullara tam giriş", rehberlik);
        superAdmin = roleRepository.save(superAdmin);
        for (SystemModule m : allModules) {
            rolePermissionRepository.save(perm(superAdmin, m, true, true, true, true,
                    "REQUESTS".equals(m.getCode()), "COORDINATOR".equals(m.getCode())));
        }

        // 2. Satış Meneceri
        Role salesRole = role("Satış Meneceri", "Müştəri və sorğu idarəetməsi", satis);
        salesRole = roleRepository.save(salesRole);
        grantAll(salesRole, byCode, "CUSTOMER_MANAGEMENT");
        grantAll(salesRole, byCode, "CONTRACTOR_MANAGEMENT");
        grant(salesRole, byCode, "REQUESTS", true, true, true, true, true, false);
        grant(salesRole, byCode, "PROJECTS", true, false, false, false, false, false);
        grant(salesRole, byCode, "GARAGE", true, false, false, false, false, false);

        // 3. Koordinator
        Role coordRole = role("Koordinator", "Koordinasiya əməliyyatları", koord);
        coordRole = roleRepository.save(coordRole);
        grant(coordRole, byCode, "REQUESTS", true, false, true, false, false, false);
        grant(coordRole, byCode, "COORDINATOR", true, true, true, true, false, true);
        grant(coordRole, byCode, "GARAGE", true, false, false, false, false, false);
        grant(coordRole, byCode, "CUSTOMER_MANAGEMENT", true, false, false, false, false, false);
        grant(coordRole, byCode, "OPERATORS", true, false, false, false, false, false);

        // 4. Maliyyəçi
        Role financeRole = role("Maliyyəçi", "Mühasibatlıq və maliyyə əməliyyatları", maliyye);
        financeRole = roleRepository.save(financeRole);
        grantAll(financeRole, byCode, "ACCOUNTING");
        grant(financeRole, byCode, "PROJECTS", true, false, false, false, false, false);
        grant(financeRole, byCode, "REQUESTS", true, false, false, false, false, false);
        grant(financeRole, byCode, "AUDIT_LOG", true, false, false, false, false, false);

        // 5. Texnik
        Role techRole = role("Texnik", "Qaraj və texniki xidmət", texniki);
        techRole = roleRepository.save(techRole);
        grantAll(techRole, byCode, "GARAGE");
        grantAll(techRole, byCode, "SERVICE_MANAGEMENT");
        grant(techRole, byCode, "REQUESTS", true, false, false, false, false, false);
        grant(techRole, byCode, "OPERATORS", true, false, false, false, false, false);

        // ── İstifadəçilər ──

        // Admin
        userRepository.save(User.builder()
                .fullName("Fuad Quliyev")
                .email("admin@ces.az")
                .password(passwordEncoder.encode("Admin@123"))
                .phone("+994501000001")
                .department(rehberlik)
                .role(superAdmin)
                .hasApproval(true)
                .active(true)
                .build());

        // Satış Meneceri
        userRepository.save(User.builder()
                .fullName("Nigar Əhmədova")
                .email("nigar@ces.az")
                .password(passwordEncoder.encode("Test@123"))
                .phone("+994502000001")
                .department(satis)
                .role(salesRole)
                .active(true)
                .build());

        // Koordinator
        userRepository.save(User.builder()
                .fullName("Bəhruz Hüseynov")
                .email("behruz@ces.az")
                .password(passwordEncoder.encode("Test@123"))
                .phone("+994503000001")
                .department(koord)
                .role(coordRole)
                .active(true)
                .build());

        // Maliyyəçi
        userRepository.save(User.builder()
                .fullName("Xədicə Babayeva")
                .email("xedice@ces.az")
                .password(passwordEncoder.encode("Test@123"))
                .phone("+994504000001")
                .department(maliyye)
                .role(financeRole)
                .active(true)
                .build());

        log.info("5 şöbə, 5 rol, 4 istifadəçi əlavə edildi.");
    }

    // ─── Köməkçi metodlar ────────────────────────────────────────────────────

    private SystemModule module(String code, String az, int order) {
        return SystemModule.builder().code(code).nameAz(az).nameEn(az).orderIndex(order).build();
    }

    private Department dept(String name, String desc) {
        return Department.builder().name(name).description(desc).build();
    }

    private Role role(String name, String desc, Department dept) {
        return Role.builder().name(name).description(desc).department(dept).build();
    }

    private RolePermission perm(Role role, SystemModule m,
                                 boolean get, boolean post, boolean put, boolean del,
                                 boolean send, boolean offer) {
        return RolePermission.builder()
                .role(role).module(m)
                .canGet(get).canPost(post).canPut(put).canDelete(del)
                .canSendToCoordinator(send).canSubmitOffer(offer)
                .build();
    }

    private void grantAll(Role role, Map<String, SystemModule> byCode, String code) {
        if (!byCode.containsKey(code)) return;
        rolePermissionRepository.save(perm(role, byCode.get(code), true, true, true, true, false, false));
    }

    private void grant(Role role, Map<String, SystemModule> byCode, String code,
                       boolean get, boolean post, boolean put, boolean del, boolean send, boolean offer) {
        if (!byCode.containsKey(code)) return;
        rolePermissionRepository.save(perm(role, byCode.get(code), get, post, put, del, send, offer));
    }
}
