package com.ces.erp.accounting.dto;

import com.ces.erp.accounting.entity.InvoiceLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Qaimə sətri — həm request (yaratma), həm response (göstərmə) üçün.
 * Toplu qaimədə hər texnika bir sətirdir.
 */
@Data
@Builder
public class InvoiceLineDto {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private Long planItemId;

    private BigDecimal unitPrice;
    private Integer dayCount;

    // Aylıq iş cədvəli (könüllü)
    private Integer periodMonth;
    private Integer periodYear;
    private Integer standardDays;
    private Integer extraDays;
    private BigDecimal extraHours;
    private BigDecimal monthlyRate;
    private Integer workingDaysInMonth;
    private Integer workingHoursPerDay;
    private BigDecimal overtimeRate;

    private BigDecimal equipmentAmount;
    private BigDecimal transportAmount;
    private BigDecimal lineTotal;

    // Təhvil-təslim aktı (hər texnikanın öz akti)
    private String aktFileName;
    private boolean aktFileUploaded;

    public static InvoiceLineDto from(InvoiceLine l) {
        return InvoiceLineDto.builder()
                .id(l.getId())
                .equipmentId(l.getEquipment() != null ? l.getEquipment().getId() : null)
                .equipmentName(l.getEquipmentName())
                .equipmentCode(l.getEquipment() != null ? l.getEquipment().getEquipmentCode() : null)
                .planItemId(l.getPlanItemId())
                .unitPrice(l.getUnitPrice())
                .dayCount(l.getDayCount())
                .periodMonth(l.getPeriodMonth())
                .periodYear(l.getPeriodYear())
                .standardDays(l.getStandardDays())
                .extraDays(l.getExtraDays())
                .extraHours(l.getExtraHours())
                .monthlyRate(l.getMonthlyRate())
                .workingDaysInMonth(l.getWorkingDaysInMonth())
                .workingHoursPerDay(l.getWorkingHoursPerDay())
                .overtimeRate(l.getOvertimeRate())
                .equipmentAmount(l.getEquipmentAmount())
                .transportAmount(l.getTransportAmount())
                .lineTotal(l.getLineTotal())
                .aktFileName(l.getAktFileName())
                .aktFileUploaded(l.getAktFilePath() != null)
                .build();
    }
}
