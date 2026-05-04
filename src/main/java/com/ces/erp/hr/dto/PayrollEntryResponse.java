package com.ces.erp.hr.dto;

import com.ces.erp.hr.entity.PayrollEntry;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollEntryResponse {

    private Long id;
    private Long periodId;
    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private String employeeFin;
    private String positionName;

    private BigDecimal baseSalary;
    private Integer workingDaysInMonth;
    private Integer actualDaysWorked;
    private BigDecimal extraHours;
    private BigDecimal overtimePay;
    private BigDecimal bonus;
    private BigDecimal vacationPay;
    private BigDecimal penalty;
    private BigDecimal grossTotal;

    private BigDecimal incomeTax;
    private BigDecimal employeePension;
    private BigDecimal employeeUnemployment;
    private BigDecimal employeeMedical;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;

    private BigDecimal employerPension;
    private BigDecimal employerUnemployment;
    private BigDecimal employerMedical;
    private BigDecimal totalEmployerContributions;
    private BigDecimal totalCompanyCost;

    private String notes;

    public static PayrollEntryResponse from(PayrollEntry e) {
        return PayrollEntryResponse.builder()
                .id(e.getId())
                .periodId(e.getPeriod() != null ? e.getPeriod().getId() : null)
                .employeeId(e.getEmployee() != null ? e.getEmployee().getId() : null)
                .employeeCode(e.getEmployee() != null ? e.getEmployee().getEmployeeCode() : null)
                .employeeFullName(e.getEmployeeFullName())
                .employeeFin(e.getEmployee() != null ? e.getEmployee().getFin() : null)
                .positionName(e.getPositionName())
                .baseSalary(e.getBaseSalary())
                .workingDaysInMonth(e.getWorkingDaysInMonth())
                .actualDaysWorked(e.getActualDaysWorked())
                .extraHours(e.getExtraHours())
                .overtimePay(e.getOvertimePay())
                .bonus(e.getBonus())
                .vacationPay(e.getVacationPay())
                .penalty(e.getPenalty())
                .grossTotal(e.getGrossTotal())
                .incomeTax(e.getIncomeTax())
                .employeePension(e.getEmployeePension())
                .employeeUnemployment(e.getEmployeeUnemployment())
                .employeeMedical(e.getEmployeeMedical())
                .totalDeductions(e.getTotalDeductions())
                .netPay(e.getNetPay())
                .employerPension(e.getEmployerPension())
                .employerUnemployment(e.getEmployerUnemployment())
                .employerMedical(e.getEmployerMedical())
                .totalEmployerContributions(e.getTotalEmployerContributions())
                .totalCompanyCost(e.getTotalCompanyCost())
                .notes(e.getNotes())
                .build();
    }
}
