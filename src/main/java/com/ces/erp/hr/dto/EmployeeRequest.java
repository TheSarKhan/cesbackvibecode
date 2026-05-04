package com.ces.erp.hr.dto;

import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Ad boş ola bilməz")
    private String firstName;

    @NotBlank(message = "Soyad boş ola bilməz")
    private String lastName;

    private String fatherName;

    private String fin;
    private String idCardSeries;
    private String idCardNumber;

    private Gender gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;

    private Long positionId;
    private Long departmentId;

    @NotNull(message = "Əməkhaqqı qeyd olunmalıdır")
    @Positive(message = "Əməkhaqqı 0-dan böyük olmalıdır")
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
}
