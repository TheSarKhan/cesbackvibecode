package com.ces.erp.garage.entity;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_status_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EquipmentStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
