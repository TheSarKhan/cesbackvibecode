package com.ces.erp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Ad soyad boş ola bilməz")
    private String fullName;

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Email formatı yanlışdır")
    private String email;

    // Yalnız yaradılarkən məcburidir; update-də null olarsa şifrə dəyişdirilmir
    private String password;

    private String phone;

    @NotNull(message = "Şöbə ID boş ola bilməz")
    private Long departmentId;

    @NotNull(message = "Rol ID boş ola bilməz")
    private Long roleId;
}
