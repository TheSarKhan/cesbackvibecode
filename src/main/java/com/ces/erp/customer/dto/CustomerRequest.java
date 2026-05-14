package com.ces.erp.customer.dto;

import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class CustomerRequest {

    @NotBlank(message = "Şirkət adı tələb olunur")
    @Size(min = 2, max = 150, message = "Şirkət adı 2-150 simvol arasında olmalıdır")
    private String companyName;

    @Pattern(regexp = "^\\d{10}$", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String voen;

    @Size(max = 200, message = "Ünvan maksimum 200 simvol ola bilər")
    private String address;

    @Size(max = 100, message = "Direktor adı maksimum 100 simvol ola bilər")
    private String directorName;

    @Size(max = 100, message = "Məsul şəxsin adı maksimum 100 simvol ola bilər")
    private String supplierPerson;

    @Pattern(regexp = "^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$", message = "Düzgün telefon nömrəsi daxil edin")
    private String supplierPhone;

    @Size(max = 100, message = "Məsul şəxsin adı maksimum 100 simvol ola bilər")
    private String officeContactPerson;

    @Pattern(regexp = "^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$", message = "Düzgün telefon nömrəsi daxil edin")
    private String officeContactPhone;

    private Set<String> paymentTypes = new HashSet<>();

    private CustomerStatus status = CustomerStatus.ACTIVE;
    private RiskLevel riskLevel = RiskLevel.LOW;
    private String notes;
}
