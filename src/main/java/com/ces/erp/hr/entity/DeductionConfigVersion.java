package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Tutulma konfiqurasiyasının bir versiyası.
 *
 * <p>Hər dəyişiklik yeni versiya kimi saxlanılır (tarixçə). Hesablama zamanı dövrün tarixinə uyğun
 * versiya seçilir: {@code effective_date ≤ dövr tarixi} olan ən son versiya. UI-da "aktiv" versiya
 * cari standart kimi göstərilir.
 */
@Entity
@Table(name = "hr_deduction_config_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionConfigVersion extends BaseEntity {

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** Bu versiyanın qüvvəyə minmə tarixi. */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(length = 500)
    private String note;
}
