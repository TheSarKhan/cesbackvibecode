package com.ces.erp.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BankRequest {

    @NotBlank(message = "Bank adı tələb olunur")
    @Size(max = 200)
    private String bankName;

    @Size(max = 100)
    private String bankCode;

    @Size(max = 50)
    private String swift;

    @Size(max = 100)
    private String iban;

    @Size(max = 100)
    private String correspondentAccount;

    @Size(max = 100)
    private String settlementAccount;
}
