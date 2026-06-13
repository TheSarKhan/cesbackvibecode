package com.ces.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserPasswordRequest {

    @NotBlank(message = "Cari şifrə boş ola bilməz")
    private String currentPassword;

    @NotBlank(message = "Yeni şifrə boş ola bilməz")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Yeni şifrə minimum 8 simvol, 1 böyük hərf, 1 kiçik hərf və 1 rəqəm olmalıdır"
    )
    private String newPassword;
}
