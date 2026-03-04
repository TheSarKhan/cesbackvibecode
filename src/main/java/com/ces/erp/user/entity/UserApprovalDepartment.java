package com.ces.erp.user.entity;

import com.ces.erp.department.entity.Department;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_approval_departments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "department_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserApprovalDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
