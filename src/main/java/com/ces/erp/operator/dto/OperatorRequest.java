package com.ces.erp.operator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OperatorRequest {

    @NotBlank(message = "Ad tələb olunur")
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    private String lastName;

    private String address;
    @Pattern(
            regexp = "^(\\+994|0)?[0-9]{9}$",
            message = "Düzgün telefon nömrəsi daxil edin"
    )
    private String phone;
    @Email
    private String email;
    private String specialization;
    private String notes;
}
