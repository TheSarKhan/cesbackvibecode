package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.department.entity.Department;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hr_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    // Avtomatik kod: EMP-2026-0001
    @Column(unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String fatherName;

    // Vergi ödəyicisi şəxsiyyət nömrəsi (FIN)
    @Column(unique = true, length = 20)
    private String fin;

    // Şəxsiyyət vəsiqəsinin seriyası (AZE/AA və s.)
    private String idCardSeries;

    private String idCardNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    private String phone;

    private String email;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Aylıq gross əməkhaqqı
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossSalary;

    private LocalDate hireDate;

    private LocalDate terminationDate;

    @Column(length = 500)
    private String terminationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // Bank məlumatları (əməkhaqqı köçürüləcək hesab)
    private String bankName;
    private String bankAccount; // IBAN

    // Foto faylı (uploads/hr-employees/...)
    private String photoUrl;

    @Column(length = 1000)
    private String notes;

    // İllik məzuniyyət hüququ (gün sayı, default 21)
    @Column(nullable = false)
    @Builder.Default
    private Integer annualLeaveDays = 21;

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(lastName);
        }
        if (fatherName != null && !fatherName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(fatherName);
        }
        return sb.toString();
    }
}
