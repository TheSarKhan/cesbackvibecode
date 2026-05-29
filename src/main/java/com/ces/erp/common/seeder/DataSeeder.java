package com.ces.erp.common.seeder;

import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.permission.PermissionLabels;
import com.ces.erp.permission.entity.Permission;
import com.ces.erp.permission.repository.PermissionRepository;
import com.ces.erp.role.entity.Role;
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

import java.util.HashMap;
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
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private Map<String, String> moduleNameByCode = new HashMap<>();

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
                module("PROJECT_MANAGER",        "Layihə Meneceri",            7),
                module("COORDINATOR",            "Koordinator Modulu",         8),
                module("PROJECTS",               "Layihələr Modulu",           9),
                module("ACCOUNTING",             "Mühasibatlıq Modulu",       10),
                module("SERVICE_MANAGEMENT",     "Texniki Servis Modulu",     11),
                module("INVESTORS",              "İnvestor İdarəetməsi",      12),
                module("OPERATORS",              "Operator İdarəetməsi",      13),
                module("OPERATIONS_APPROVAL",    "Əməliyyatların Təsdiqi",    14),
                module("TRASH",                  "Silinmiş Məlumatlar",       15),
                module("AUDIT_LOG",              "Audit Jurnal",              16),
                module("CONFIG",                 "Konfiqurasiya Modulu",      17),
                module("HR_MANAGEMENT",          "İnsan Resursları Modulu",   18)
        );
        moduleRepository.saveAll(modules);
        log.info("{} modul əlavə edildi.", modules.size());
    }

    // ─── Şöbələr, rollar, istifadəçilər ──────────────────────────────────────

    private void seedDepartmentsRolesUsers() {
        moduleRepository.findAll().forEach(m -> moduleNameByCode.put(m.getCode(), m.getNameAz()));

        // ── Şöbələr ──
        Department rehberlik  = dept("Rəhbərlik",            "Şirkət rəhbərliyi");
        Department satis      = dept("Satış şöbəsi",         "Müştəri və sorğu idarəetməsi");
        Department layihePm   = dept("Layihə İdarəetməsi",   "Layihə menecerləri");
        Department koord      = dept("Koordinasiya şöbəsi",  "Koordinator əməliyyatları");
        Department maliyye    = dept("Maliyyə şöbəsi",       "Mühasibatlıq və maliyyə");
        Department texniki    = dept("Texniki Xidmət şöbəsi","Qaraj və avadanlıq idarəetməsi");
        departmentRepository.saveAll(List.of(rehberlik, satis, layihePm, koord, maliyye, texniki));

        // ── Rollar ──

        // 1. Super Admin — adi rol, BÜTÜN real icazələr qrant kimi verilir (xüsusi flag yox)
        Role superAdmin = role("Super Admin", "Bütün modullara tam giriş", rehberlik);
        grantAllReal(superAdmin);
        roleRepository.save(superAdmin);

        // 2. Satış Meneceri
        Role salesRole = role("Satış Meneceri", "Müştəri və sorğu idarəetməsi", satis);
        grant(salesRole, "CUSTOMER_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(salesRole, "CONTRACTOR_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(salesRole, "REQUESTS", "GET", "POST", "PUT", "DELETE", "SEND_COORDINATOR");
        grant(salesRole, "PROJECTS", "GET");
        grant(salesRole, "GARAGE", "GET");
        roleRepository.save(salesRole);

        // 3. Layihə Meneceri
        Role pmRole = role("Layihə Meneceri", "Layihə menecmenti və müştəri əlaqələri", layihePm);
        grant(pmRole, "REQUESTS", "GET", "PUT");
        grant(pmRole, "PROJECT_MANAGER", "GET", "POST", "PUT", "DELETE", "APPROVE_PM");
        grant(pmRole, "CUSTOMER_MANAGEMENT", "GET");
        grant(pmRole, "CONTRACTOR_MANAGEMENT", "GET");
        grant(pmRole, "INVESTORS", "GET");
        grant(pmRole, "GARAGE", "GET");
        grant(pmRole, "PROJECTS", "GET");
        roleRepository.save(pmRole);

        // 4. Koordinator
        Role coordRole = role("Koordinator", "Koordinasiya əməliyyatları", koord);
        grant(coordRole, "REQUESTS", "GET", "PUT");
        grant(coordRole, "COORDINATOR", "GET", "POST", "PUT", "DELETE", "SUBMIT_OFFER", "DISPATCH", "DELIVER");
        grant(coordRole, "GARAGE", "GET");
        grant(coordRole, "CUSTOMER_MANAGEMENT", "GET");
        grant(coordRole, "CONTRACTOR_MANAGEMENT", "GET");
        grant(coordRole, "OPERATORS", "GET");
        grant(coordRole, "PROJECT_MANAGER", "GET");
        roleRepository.save(coordRole);

        // 5. Maliyyəçi
        Role financeRole = role("Maliyyəçi", "Mühasibatlıq və maliyyə əməliyyatları", maliyye);
        grant(financeRole, "ACCOUNTING", "GET", "POST", "PUT", "DELETE", "CHECK_DOCUMENTS");
        grant(financeRole, "PROJECTS", "GET");
        grant(financeRole, "REQUESTS", "GET");
        grant(financeRole, "AUDIT_LOG", "GET");
        roleRepository.save(financeRole);

        // 6. Texnik
        Role techRole = role("Texnik", "Qaraj və texniki xidmət", texniki);
        grant(techRole, "GARAGE", "GET", "POST", "PUT", "DELETE");
        grant(techRole, "SERVICE_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(techRole, "REQUESTS", "GET");
        grant(techRole, "OPERATORS", "GET");
        roleRepository.save(techRole);

        // ── İstifadəçilər (hər rol üçün bir test useri) ──
        saveUser("Fuad Quliyev", "admin@ces.az", "Admin@123", "+994501000001", rehberlik, superAdmin, true);
        saveUser("Nigar Əhmədova", "nigar@ces.az", "Test@123", "+994502000001", satis, salesRole, false);
        saveUser("Səbinə Quliyeva", "sebine@ces.az", "Test@123", "+994505000001", layihePm, pmRole, false);
        saveUser("Bəhruz Hüseynov", "behruz@ces.az", "Test@123", "+994503000001", koord, coordRole, false);
        saveUser("Xədicə Babayeva", "xedice@ces.az", "Test@123", "+994504000001", maliyye, financeRole, false);
        saveUser("Elvin Texnik", "texnik@ces.az", "Test@123", "+994506000001", texniki, techRole, false);

        log.info("6 şöbə, 6 rol, 6 istifadəçi əlavə edildi.");
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

    /** Super Admin üçün — bütün real (@PreAuthorize) icazələr. */
    private void grantAllReal(Role role) {
        grant(role, "CUSTOMER_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(role, "CONTRACTOR_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(role, "INVESTORS", "GET", "POST", "PUT", "DELETE");
        grant(role, "OPERATORS", "GET", "POST", "PUT", "DELETE");
        grant(role, "GARAGE", "GET", "POST", "PUT", "DELETE");
        grant(role, "REQUESTS", "GET", "POST", "PUT", "DELETE", "SEND_COORDINATOR");
        grant(role, "PROJECT_MANAGER", "GET", "POST", "PUT", "DELETE", "APPROVE_PM");
        grant(role, "COORDINATOR", "GET", "POST", "PUT", "DELETE", "SUBMIT_OFFER", "DISPATCH", "DELIVER");
        grant(role, "PROJECTS", "GET", "POST", "PUT", "DELETE");
        grant(role, "ACCOUNTING", "GET", "POST", "PUT", "DELETE", "CHECK_DOCUMENTS");
        grant(role, "HR_MANAGEMENT", "GET", "POST", "PUT", "DELETE");
        grant(role, "ROLE_PERMISSION", "GET", "POST", "PUT", "DELETE");
        grant(role, "OPERATIONS_APPROVAL", "GET", "PUT");
        grant(role, "CONFIG", "GET", "POST", "PUT", "DELETE", "PING");
        grant(role, "AUDIT_LOG", "GET");
        grant(role, "TRASH", "GET", "PUT");
    }

    /** Verilmiş action-lar üçün icazələri (kataloqda yoxdursa yaradaraq) rola əlavə edir. */
    private void grant(Role role, String moduleCode, String... actions) {
        for (String action : actions) {
            String code = moduleCode + ":" + action;
            Permission p = permissionRepository.findByCode(code).orElseGet(() ->
                    permissionRepository.save(Permission.builder()
                            .code(code)
                            .moduleCode(moduleCode)
                            .action(action)
                            .labelAz(PermissionLabels.fullLabel(moduleNameByCode.get(moduleCode), action))
                            .autoDiscovered(false)
                            .build()));
            role.getGrantedPermissions().add(p);
        }
    }

    private void saveUser(String fullName, String email, String password, String phone,
                          Department dept, Role role, boolean hasApproval) {
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phone(phone)
                .department(dept)
                .hasApproval(hasApproval)
                .active(true)
                .build();
        user.getRoles().add(role);
        userRepository.save(user);
    }
}
