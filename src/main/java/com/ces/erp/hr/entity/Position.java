package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.department.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "hr_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    // Default əməkhaqqı (yeni işçi əlavə edildikdə təklif olunur)
    @Column(precision = 19, scale = 2)
    private BigDecimal defaultSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
