package com.ces.erp.project.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.request.entity.TechRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Column(length = 20, unique = true)
    private String projectCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private TechRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PENDING;

    private LocalDate startDate;
    private LocalDate endDate;

    // Müqavilə
    @Column(nullable = false)
    @Builder.Default
    private boolean hasContract = false;

    private String contractFilePath;
    private String contractFileName;

    // Bağlanış məlumatları
    @Column(precision = 12, scale = 2)
    private BigDecimal evacuationCost;

    @Column(precision = 8, scale = 2)
    private BigDecimal scheduledHours;

    @Column(precision = 8, scale = 2)
    private BigDecimal actualHours;

    // Maliyyə
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectExpense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectRevenue> revenues = new ArrayList<>();
}
