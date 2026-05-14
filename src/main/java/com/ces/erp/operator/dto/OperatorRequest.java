package com.ces.erp.operator.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OperatorRequest {

    @NotBlank(message = "Ad tələb olunur")
    @Size(min = 2, max = 50, message = "Ad 2-50 simvol arasında olmalıdır")
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    @Size(min = 2, max = 50, message = "Soyad 2-50 simvol arasında olmalıdır")
    private String lastName;

    @Size(max = 200, message = "Ünvan maksimum 200 simvol ola bilər")
    private String address;

    @Pattern(regexp = "^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$", message = "Düzgün telefon nömrəsi daxil edin")
    private String phone;

    @Email(message = "Düzgün email formatı daxil edin")
    private String email;

    @Size(max = 100, message = "İxtisas maksimum 100 simvol ola bilər")
    private String specialization;

    @Size(max = 500, message = "Qeyd maksimum 500 simvol ola bilər")
    private String notes;
}
