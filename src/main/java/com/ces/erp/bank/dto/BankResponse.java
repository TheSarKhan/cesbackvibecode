package com.ces.erp.bank.dto;

import com.ces.erp.bank.entity.Bank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BankResponse {

    private Long id;
    private String bankName;
    private String bankCode;
    private String swift;
    private String iban;
    private String correspondentAccount;
    private String settlementAccount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BankResponse from(Bank b) {
        return BankResponse.builder()
                .id(b.getId())
                .bankName(b.getBankName())
                .bankCode(b.getBankCode())
                .swift(b.getSwift())
                .iban(b.getIban())
                .correspondentAccount(b.getCorrespondentAccount())
                .settlementAccount(b.getSettlementAccount())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
