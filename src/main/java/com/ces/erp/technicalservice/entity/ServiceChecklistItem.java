package com.ces.erp.technicalservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_checklist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_record_id", nullable = false)
    private ServiceRecord serviceRecord;

    @Column(nullable = false)
    private String itemName;

    private boolean checked;

    private String note;
}
