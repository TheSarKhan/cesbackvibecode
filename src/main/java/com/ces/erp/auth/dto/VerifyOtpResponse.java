package com.ces.erp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class VerifyOtpResponse {
    private String verificationToken;
    private String message;
}
