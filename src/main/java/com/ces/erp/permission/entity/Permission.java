package com.ces.erp.permission.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Dinamik icazə kataloqu. `code` ({@code MODULE:ACTION}) tək həqiqi açardır və
 * birbaşa {@code @PreAuthorize("hasAuthority('MODULE:ACTION')")} string-i ilə üst-üstə düşür.
 * Yeni endpoint etiketlənəndə {@code PermissionScanner} bu kataloqa avtomatik upsert edir.
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;          // məs. ACCOUNTING:CHECK_DOCUMENTS

    @Column(nullable = false)
    private String moduleCode;    // məs. ACCOUNTING

    @Column(nullable = false)
    private String action;        // məs. CHECK_DOCUMENTS

    @Column(nullable = false)
    private String labelAz;       // ilk dəfə humanize, sonra admin redaktə edir

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean autoDiscovered = false;
}
