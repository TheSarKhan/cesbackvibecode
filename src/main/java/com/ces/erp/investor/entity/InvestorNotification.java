package com.ces.erp.investor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** İnvestora göndərilmiş bildiriş (tarixçə + oxunma vəziyyəti). */
@Entity
@Table(name = "investor_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class InvestorNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(length = 40)
    private String type;        // EQUIPMENT_RENTED, PAYMENT_RECEIVED, ...

    @Column(name = "related_id")
    private Long relatedId;     // texnika / ödəniş id (naviqasiya üçün)

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
