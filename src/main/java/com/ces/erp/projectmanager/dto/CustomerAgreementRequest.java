package com.ces.erp.projectmanager.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerAgreementRequest {

    // Texnika üçün razılaşdırılmış qiymət
    private BigDecimal agreedEquipmentPrice;

    // Daşınma üçün razılaşdırılmış qiymət
    private BigDecimal agreedTransportPrice;

    // Cəmi (opsional — backend hesablaya bilər)
    private BigDecimal agreedTotalPrice;

    // Razılaşma haqqında qeyd (sorğunun notes-una əlavə olunur)
    private String agreementNote;
}
