package com.ces.erp.common.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;   // "MÜŞTƏRİ", "SORĞU", "LAYİHƏ", "FAKTURA" etc.

    private Long entityId;

    private String entityLabel;  // human-readable name (e.g., company name, request code)

    @Column(nullable = false)
    private String action;       // "YARADILDI", "YENİLƏNDİ", "SİLİNDİ", "BƏRPA EDİLDİ"

    private String performedBy;  // user's full name or email

    @Column(nullable = false)
    private LocalDateTime performedAt;

    private String summary;      // short description of what changed
}
