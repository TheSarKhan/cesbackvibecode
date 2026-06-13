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
import com.ces.erp.user.dto.UserContactRequest;
import com.ces.erp.user.dto.UserPasswordRequest;
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

import java.util.List;

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

        Role role = roleRepository.findByIdAndDeletedFalse(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", request.getRoleId()));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .department(dept)
                .role(role)
                .build();

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

        Role role = roleRepository.findByIdAndDeletedFalse(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", request.getRoleId()));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDepartment(dept);
        user.setRole(role);

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

    @Transactional
    public void toggleActive(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    // ───── Self-service (/me) ─────────────────────────────────────

    public UserResponse getCurrent(Long currentUserId) {
        User user = userRepository.findByIdAndDeletedFalse(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", currentUserId));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyContact(Long currentUserId, UserContactRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", currentUserId));

        String newEmail = request.getEmail() != null ? request.getEmail().trim() : null;
        if (newEmail == null || newEmail.isBlank()) {
            throw new BusinessException("Email boş ola bilməz");
        }
        if (!newEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailAndDeletedFalse(newEmail)) {
            throw new DuplicateResourceException("Bu email artıq istifadə edilir");
        }

        String newPhone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (newPhone != null && newPhone.isBlank()) newPhone = null;

        user.setEmail(newEmail);
        user.setPhone(newPhone);
        User saved = userRepository.save(user);
        auditService.log("İSTİFADƏÇİ", saved.getId(), saved.getFullName(),
                "ƏLAQƏ_YENİLƏNDİ", "İstifadəçi öz əlaqə məlumatlarını yenilədi");
        return UserResponse.from(saved);
    }

    @Transactional
    public void updateMyPassword(Long currentUserId, UserPasswordRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", currentUserId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Cari şifrə yanlışdır");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Yeni şifrə cari şifrə ilə eyni ola bilməz");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditService.log("İSTİFADƏÇİ", user.getId(), user.getFullName(),
                "ŞİFRƏ_YENİLƏNDİ", "İstifadəçi öz şifrəsini dəyişdi");
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
