package com.ces.erp.hr.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollEntryRequest {
    // Müraciətlə dəyişdirilə bilən sahələr (gross salary entry yaranan zaman snapshot olunur)
    private Integer actualDaysWorked;
    private BigDecimal extraHours;
    private BigDecimal overtimePay;
    private BigDecimal bonus;
    private BigDecimal vacationPay;
    private BigDecimal penalty;
    private BigDecimal baseSalary; // overrride etmək üçün
    private String notes;
}
