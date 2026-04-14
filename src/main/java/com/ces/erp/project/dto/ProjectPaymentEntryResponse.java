package com.ces.erp.project.dto;

import com.ces.erp.project.entity.ProjectPaymentEntry;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectPaymentEntryResponse {

    private Long id;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;
    private boolean closed;
    private LocalDateTime createdAt;

    public static ProjectPaymentEntryResponse from(ProjectPaymentEntry e) {
        return ProjectPaymentEntryResponse.builder()
                .id(e.getId())
                .amount(e.getAmount())
                .paymentDate(e.getPaymentDate())
                .note(e.getNote())
                .closed(e.isClosed())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
