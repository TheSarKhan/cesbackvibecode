package com.ces.erp.operator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OperatorRequest {

    @NotBlank(message = "Ad tələb olunur")
    private String firstName;

    @NotBlank(message = "Soyad tələb olunur")
    private String lastName;

    private String address;
    private String phone;
    private String email;
    private String specialization;
    private String notes;
}
