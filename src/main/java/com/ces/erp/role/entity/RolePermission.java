package com.ces.erp.role.entity;

import com.ces.erp.systemmodule.entity.SystemModule;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "module_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "module_id", nullable = false)
    private SystemModule module;

    @Column(nullable = false)
    @Builder.Default
    private boolean canGet = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canPost = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canPut = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean canDelete = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canSendToCoordinator = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canSubmitOffer = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canSendToAccounting = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canReturnToProject = false;

    // ─── Yeni flow icazələri ─────────────────────────────────────────────────

    // PM-in qiymət razılaşmasını təsdiqləyib layihə yaratması (status 7→8)
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canApproveByPm = false;

    // Mühasibatlığın sənəd yoxlamasını tamamlaması (status 9→10)
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canCheckDocuments = false;

    // Koordinatorun yükləmə əməliyyatı (status 11→12)
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canDispatch = false;

    // Koordinatorun təhvil-təslim əməliyyatı (status 12→13)
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean canDeliver = false;
}
