package com.ces.erp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Ad soyad boş ola bilməz")
    private String fullName;

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Email formatı yanlışdır")
    private String email;

    private String password;

    @Pattern(
            regexp = "^(\\+994|0)?[0-9]{9}$",
            message = "Düzgün telefon nömrəsi daxil edin"
    )
    private String phone;

    @NotNull(message = "Şöbə ID boş ola bilməz")
    private Long departmentId;

    @NotNull(message = "Rol ID boş ola bilməz")
    private Long roleId;
}
