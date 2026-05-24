package com.ces.erp.projectmanager.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.request.entity.TechRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "request_shortlists", indexes = {
        @Index(name = "idx_shortlist_request", columnList = "request_id"),
        @Index(name = "idx_shortlist_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestShortlist extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private TechRequest request;

    @OneToMany(mappedBy = "shortlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShortlistItem> items = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;
}
