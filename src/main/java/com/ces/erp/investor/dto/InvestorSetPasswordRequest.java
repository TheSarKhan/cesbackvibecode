package com.ces.erp.investor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin tərəfi — başlanğıc/reset şifrə təyini. */
@Data
public class InvestorSetPasswordRequest {

    @NotBlank(message = "Şifrə boş ola bilməz")
    @Size(min = 8, message = "Şifrə ən azı 8 simvol olmalıdır")
    private String password;
}
