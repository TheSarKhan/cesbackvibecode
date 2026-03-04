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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
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
        seedModules();
        seedAdminAccount();
    }

    // ─── 10 Sistem modulu (BRD-dən) ───────────────────────────────────────────

    private void seedModules() {
        if (moduleRepository.count() > 0) return;
        log.info("Sistem modulları seed edilir...");

        List<SystemModule> modules = List.of(
                module("CUSTOMER_MANAGEMENT",    "Müştəri İdarəetməsi",      "Customer Management",         1),
                module("CONTRACTOR_MANAGEMENT",  "Podratçı İdarəetməsi",     "Contractor Management",       2),
                module("ROLE_PERMISSION",        "Rol və İcazə İdarəetməsi", "Role & Permission Management",3),
                module("EMPLOYEE_MANAGEMENT",    "İşçi İdarəetməsi",         "Employee Management",         4),
                module("GARAGE",                 "Qaraj Modulu",             "Garage / Fleet Management",   5),
                module("REQUESTS",               "Sorğular Modulu",          "Requests Module",             6),
                module("COORDINATOR",            "Koordinator Modulu",       "Coordinator Module",          7),
                module("PROJECTS",               "Layihələr Modulu",         "Projects Module",             8),
                module("ACCOUNTING",             "Mühasibatlıq Modulu",      "Accounting Module",           9),
                module("SERVICE_MANAGEMENT",     "Texniki Servis Modulu",    "Service Management",         10)
        );

        moduleRepository.saveAll(modules);
        log.info("{} modul əlavə edildi.", modules.size());
    }

    // ─── Super Admin hesabı ────────────────────────────────────────────────────

    private void seedAdminAccount() {
        if (userRepository.existsByEmailAndDeletedFalse("admin@ces.az")) return;
        log.info("Admin hesabı seed edilir...");

        // 1. Şöbə
        Department dept = departmentRepository.findAllByDeletedFalse().stream()
                .filter(d -> d.getName().equals("Rəhbərlik"))
                .findFirst()
                .orElseGet(() -> departmentRepository.save(
                        Department.builder().name("Rəhbərlik").description("Şirkət rəhbərliyi").build()
                ));

        // 2. Rol
        Role adminRole = Role.builder()
                .name("Super Admin")
                .description("Bütün modullara tam giriş")
                .department(dept)
                .build();
        adminRole = roleRepository.save(adminRole);

        // 3. Bütün modullara tam icazə
        List<SystemModule> allModules = moduleRepository.findAll();
        final Role finalRole = adminRole;
        List<RolePermission> permissions = allModules.stream()
                .map(m -> RolePermission.builder()
                        .role(finalRole)
                        .module(m)
                        .canGet(true)
                        .canPost(true)
                        .canPut(true)
                        .canDelete(true)
                        .build())
                .toList();
        rolePermissionRepository.saveAll(permissions);

        // 4. İstifadəçi
        User admin = User.builder()
                .fullName("Super Admin")
                .email("admin@ces.az")
                .password(passwordEncoder.encode("Admin@123"))
                .department(dept)
                .role(adminRole)
                .hasApproval(true)
                .active(true)
                .build();
        userRepository.save(admin);

        log.info("Admin hesabı yaradıldı → admin@ces.az / Admin@123");
    }

    private SystemModule module(String code, String az, String en, int order) {
        return SystemModule.builder()
                .code(code)
                .nameAz(az)
                .nameEn(en)
                .orderIndex(order)
                .build();
    }
}
