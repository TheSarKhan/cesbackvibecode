package com.ces.erp.user.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserApprovalDepartmentRepository approvalDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAll() {
        return userRepository.findAllByDeletedFalse().stream()
                .map(UserResponse::from)
                .toList();
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
            throw new BusinessException("Bu email artıq istifadə edilir");
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

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));

        // Email dəyişibsə unikallıq yoxla
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BusinessException("Bu email artıq istifadə edilir");
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

        return UserResponse.from(userRepository.save(user));
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

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
        user.softDelete();
        userRepository.save(user);
    }
}
