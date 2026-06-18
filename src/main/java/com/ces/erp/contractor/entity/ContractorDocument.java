package com.ces.erp.contractor.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "contractor_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractorDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;

    @Column(nullable = false)
    private String filePath;

    @Column(length = 255)
    private String documentName;

    @Column(length = 50)
    private String fileType;

    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
}
