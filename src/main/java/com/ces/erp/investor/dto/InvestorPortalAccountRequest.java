package com.ces.erp.investor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin tərəfi — portal hesab maili + aktiv/passiv. */
@Data
public class InvestorPortalAccountRequest {

    @Email(message = "Email formatı yanlışdır")
    @Size(max = 255, message = "Email maksimum 255 simvol ola bilər")
    private String accountEmail;   // könüllü; boşdursa silinir

    private boolean portalEnabled;
}
