package com.ces.erp.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserRequest {

    @NotBlank(message = "Ad soyad boş ola bilməz")
    @Size(min = 2, max = 100, message = "Ad soyad 2-100 simvol arasında olmalıdır")
    private String fullName;

    @NotBlank(message = "Email boş ola bilməz")
    @Email(message = "Email formatı yanlışdır")
    private String email;

    @Pattern(
            regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Şifrə minimum 8 simvol, 1 böyük hərf, 1 kiçik hərf və 1 rəqəm olmalıdır"
    )
    private String password;

    @Pattern(
            regexp = "^$|^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$",
            message = "Düzgün telefon nömrəsi daxil edin"
    )
    private String phone;

    @NotNull(message = "Şöbə ID boş ola bilməz")
    private Long departmentId;

    @NotEmpty(message = "Ən azı bir rol seçilməlidir")
    private List<Long> roleIds;
}
