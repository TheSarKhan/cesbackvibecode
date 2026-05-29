package com.ces.erp.hr.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.DeductionAppliesTo;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tutulma növü — GELIR_VERGISI / DSMF / ISH / ITS və ya istifadəçi tərəfindən əlavə edilən yeni növ.
 *
 * <p>Aralıqlar (dərəcələr/hədlər) versiyaya görə {@link DeductionBracket}-də saxlanılır;
 * növün özü (kod, ad, taraf) versiyalardan asılı deyil.
 */
@Entity
@Table(name = "hr_deduction_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionType extends BaseEntity {

    /** Texniki kod (GELIR_VERGISI, DSMF, ISH, ITS, ...). Hesablamada sütun map-ı üçün istifadə olunur. */
    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 20)
    private DeductionAppliesTo appliesTo;

    /** İşçi tərəfi net (ödənilməli) məbləğdən çıxılırmı? İşəgötürən tərəfi heç vaxt net-dən çıxılmır. */
    @Column(name = "deducted_from_net", nullable = false)
    @Builder.Default
    private boolean deductedFromNet = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
