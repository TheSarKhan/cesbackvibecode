package com.ces.erp.investor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenRequest {

    @NotBlank(message = "Token boş ola bilməz")
    private String token;

    private String platform;   // "ios" | "android"
}
