package com.ces.erp.systemmodule.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemModule extends BaseEntity {

    // Unikal kod — icazə yoxlanışında istifadə edilir
    // Nümunə: "CUSTOMER_MANAGEMENT", "GARAGE"
    @Column(nullable = false, unique = true)
    private String code;


    @Column(nullable = false)
    private String nameAz;

    @Column(nullable = false)
    private String nameEn;

    // Sidebar-da göstərilmə sırası
    @Column(nullable = false)
    private Integer orderIndex;
}
