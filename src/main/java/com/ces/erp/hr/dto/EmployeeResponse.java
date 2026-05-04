package com.ces.erp.hr.dto;

import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.Gender;
import com.ces.erp.hr.entity.Employee;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fatherName;
    private String fullName;
    private String fin;
    private String idCardSeries;
    private String idCardNumber;
    private Gender gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;

    private Long positionId;
    private String positionName;
    private Long departmentId;
    private String departmentName;

    private BigDecimal grossSalary;

    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String terminationReason;
    private EmployeeStatus status;

    private String bankName;
    private String bankAccount;

    private String photoUrl;
    private String notes;
    private Integer annualLeaveDays;

    private LocalDateTime createdAt;

    public static EmployeeResponse from(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .fatherName(e.getFatherName())
                .fullName(e.getFullName())
                .fin(e.getFin())
                .idCardSeries(e.getIdCardSeries())
                .idCardNumber(e.getIdCardNumber())
                .gender(e.getGender())
                .birthDate(e.getBirthDate())
                .phone(e.getPhone())
                .email(e.getEmail())
                .address(e.getAddress())
                .positionId(e.getPosition() != null ? e.getPosition().getId() : null)
                .positionName(e.getPosition() != null ? e.getPosition().getName() : null)
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .grossSalary(e.getGrossSalary())
                .hireDate(e.getHireDate())
                .terminationDate(e.getTerminationDate())
                .terminationReason(e.getTerminationReason())
                .status(e.getStatus())
                .bankName(e.getBankName())
                .bankAccount(e.getBankAccount())
                .photoUrl(e.getPhotoUrl())
                .notes(e.getNotes())
                .annualLeaveDays(e.getAnnualLeaveDays())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
