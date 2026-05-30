package com.ces.erp.investor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvestorChangePasswordRequest {

    @NotBlank(message = "Köhnə şifrə boş ola bilməz")
    private String oldPassword;

    @NotBlank(message = "Yeni şifrə boş ola bilməz")
    @Size(min = 8, message = "Şifrə ən azı 8 simvol olmalıdır")
    private String newPassword;
}
