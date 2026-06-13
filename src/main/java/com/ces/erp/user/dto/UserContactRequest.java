package com.ces.erp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserContactRequest {

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Email formatı yanlışdır")
    private String email;

    @Pattern(
            regexp = "^$|^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$",
            message = "Düzgün Azərbaycan telefon nömrəsi daxil edin (məs: +994501234567)"
    )
    private String phone;
}
