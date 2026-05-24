package com.ces.erp.projectmanager.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerAgreementRequest {

    // Sifarişçi ilə razılaşdırılmış son qiymət (texnika + daşınma cəmi)
    private BigDecimal agreedTotalPrice;

    // Razılaşma haqqında qeyd (sorğunun notes-una əlavə olunur)
    private String agreementNote;
}
