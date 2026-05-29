package com.ces.erp.user.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.department.entity.Department;
import com.ces.erp.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // İstifadəçiyə bir və ya bir neçə rol (effektiv icazə bütün rolların birləşməsidir)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Approval icazəsi — hansı şöbələri approve edə bilər
    @Column(nullable = false)
    @Builder.Default
    private boolean hasApproval = false;

    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserApprovalDepartment> approvalDepartments = new ArrayList<>();
}
