package com.ces.erp.accounting.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentPreviewRequest {
    private List<Long> invoiceIds;
}
