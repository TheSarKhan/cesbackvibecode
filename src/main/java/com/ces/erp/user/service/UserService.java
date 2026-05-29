package com.ces.erp.user.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.role.entity.Role;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.user.dto.UserApprovalRequest;
import com.ces.erp.user.dto.UserRequest;
import com.ces.erp.user.dto.UserResponse;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.entity.UserApprovalDepartment;
import com.ces.erp.user.repository.UserApprovalDepartmentRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements ApprovalHandler {

    private final UserRepository userRepository;
    private final UserApprovalDepartmentRepository approvalDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "USER"; }
    @Override public String getModuleCode()  { return "ROLE_PERMISSION"; }
    @Override public String getLabel(Long id) {
        return userRepository.findByIdAndDeletedFalse(id).map(User::getFullName).orElse("İstifadəçi #" + id);
    }
    @Override public Object getSnapshot(Long id) {
        return UserResponse.from(userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id)));
    }

    /**
     * EDIT təsdiq diff-i üçün "sonrakı" snapshot — getSnapshot (UserResponse) ilə eyni formada:
     * roleIds → rol adları, departmentId → şöbə adı; parol heç vaxt daxil edilmir.
     */
    @Override
    public Object getAfterSnapshot(Long id, Object request) {
        if (!(request instanceof UserRequest req)) return null;
        User user = userRepository.findByIdAndDeletedFalse(id).orElse(null);
        if (user == null) return null;

        String deptName = req.getDepartmentId() != null
                ? departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId()).map(Department::getName).orElse(null)
                : null;

        List<Role> roles = req.getRoleIds() == null ? List.of()
                : req.getRoleIds().stream()
                    .map(rid -> roleRepository.findByIdAndDeletedFalse(rid).orElse(null))
                    .filter(java.util.Objects::nonNull).toList();
        Set<String> codes = new LinkedHashSet<>();
        roles.forEach(r -> {
            if (r.getGrantedPermissions() != null) r.getGrantedPermissions().forEach(p -> codes.add(p.getCode()));
        });
        Role primary = roles.stream().findFirst().orElse(null);

        List<UserResponse.ApprovalDeptInfo> approvalDepts = user.getApprovalDepartments() == null ? List.of()
                : user.getApprovalDepartments().stream()
                    .map(ad -> UserResponse.ApprovalDeptInfo.builder()
                            .id(ad.getDepartment().getId()).name(ad.getDepartment().getName()).build())
                    .toList();

        return UserResponse.builder()
                .id(user.getId())
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .departmentId(req.getDepartmentId())
                .departmentName(deptName)
                .roleIds(roles.stream().map(Role::getId).toList())
                .roleNames(roles.stream().map(Role::getName).toList())
                .roleId(primary != null ? primary.getId() : null)
                .roleName(primary != null ? primary.getName() : null)
                .active(user.isActive())
                .hasApproval(user.isHasApproval())
                .approvalDepartments(approvalDepts)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .permissions(codes.stream().sorted().toList())
                .build();
    }
    @Override
    public void applyEdit(Long id, String json) {
        try {
            UserRequest req = objectMapper.readValue(json, UserRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }
    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<UserResponse> getAll() {
        return userRepository.findAllByDeletedFalse().stream()
                .map(UserResponse::from)
                .toList();
    }

    public PagedResponse<UserResponse> getAllPaged(int page, int size, String search, Long departmentId) {
        String q = (search != null && !search.isBlank()) ? search : null;
        var pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        return PagedResponse.from(userRepository.findAllFiltered(q, departmentId, pageable), UserResponse::from);
    }

    // Cari istifadəçinin öz profili — rol/icazə təzələnməsi üçün (logout etmədən).
    // grantedPermissions eager fetch ilə yüklənir; istənilən authenticated user çağıra bilər.
    @Transactional(readOnly = true)
    public UserResponse getMe(String email) {
        User user = userRepository.findByEmailWithPermissions(email)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı"));
        return UserResponse.from(user);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Şifrə boş ola bilməz");
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateResourceException("Bu email artıq istifadə edilir");
        }

        Department dept = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", request.getDepartmentId()));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .department(dept)
                .build();
        user.getRoles().addAll(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);
        auditService.log("İSTİFADƏÇİ", saved.getId(), saved.getFullName(), "YARADILDI", "Yeni istifadəçi qeydiyyatı");
        return UserResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "USER")
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));

        if (!user.isActive()) {
            throw new BusinessException("Deaktiv istifadəçiyə şöbə və rol təyin edilə bilməz");
        }

        // Email dəyişibsə unikallıq yoxla
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateResourceException("Bu email artıq istifadə edilir");
        }

        Department dept = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", request.getDepartmentId()));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDepartment(dept);
        user.getRoles().clear();
        user.getRoles().addAll(resolveRoles(request.getRoleIds()));

        // Şifrə verilmiş olarsa yenilə
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        auditService.log("İSTİFADƏÇİ", updated.getId(), updated.getFullName(), "YENİLƏNDİ", "İstifadəçi məlumatları yeniləndi");
        return UserResponse.from(updated);
    }

    @Transactional
    public UserResponse updateApproval(Long id, UserApprovalRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));

        user.setHasApproval(request.isHasApproval());

        // Köhnə approval şöbələrini sil
        approvalDepartmentRepository.deleteAllByUserId(id);

        // Yeni approval şöbələrini əlavə et
        if (request.isHasApproval() && request.getApprovalDepartmentIds() != null) {
            List<UserApprovalDepartment> approvals = request.getApprovalDepartmentIds().stream()
                    .map(deptId -> {
                        Department dept = departmentRepository.findByIdAndDeletedFalse(deptId)
                                .orElseThrow(() -> new ResourceNotFoundException("Şöbə", deptId));
                        return UserApprovalDepartment.builder()
                                .user(user)
                                .department(dept)
                                .build();
                    })
                    .toList();
            approvalDepartmentRepository.saveAll(approvals);
        }

        return UserResponse.from(userRepository.save(user));
    }

    /** roleId siyahısını Role dəstinə çevirir (ən azı 1 tələb olunur). */
    private Set<Role> resolveRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException("Ən azı bir rol seçilməlidir");
        }
        Set<Role> roles = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            roles.add(roleRepository.findByIdAndDeletedFalse(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol", roleId)));
        }
        return roles;
    }

    @Transactional
    public void toggleActive(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    @RequiresApproval(module = "ROLE_PERMISSION", entityType = "USER", isDelete = true)
    public void delete(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
        auditService.log("İSTİFADƏÇİ", user.getId(), user.getFullName(), "SİLİNDİ", "İstifadəçi silindi");
        user.softDelete();
        userRepository.save(user);
    }
}
