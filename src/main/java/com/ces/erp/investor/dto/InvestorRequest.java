package com.ces.erp.investor.dto;

import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvestorRequest {

    @NotBlank(message = "Şirkət adı boş ola bilməz")
    @Size(min = 2, max = 150, message = "Şirkət adı 2-150 simvol arasında olmalıdır")
    private String companyName;

    @Pattern(regexp = "^\\d{10}$", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String voen;

    @Size(max = 100, message = "Əlaqə şəxsinin adı maksimum 100 simvol ola bilər")
    private String contactPerson;

    @Pattern(regexp = "^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$", message = "Düzgün telefon nömrəsi daxil edin")
    private String contactPhone;

    @Size(max = 200, message = "Ünvan maksimum 200 simvol ola bilər")
    private String address;

    @Size(max = 50, message = "Ödəniş növü maksimum 50 simvol ola bilər")
    private String paymentType;

    @NotNull(message = "Status boş ola bilməz")
    private ContractorStatus status;

    @DecimalMin(value = "0.00", message = "Reytinq 0-dan az ola bilməz")
    @DecimalMax(value = "5.00", message = "Reytinq 5-dən çox ola bilməz")
    private BigDecimal rating;

    @NotNull(message = "Risk səviyyəsi boş ola bilməz")
    private RiskLevel riskLevel;

    private String notes;
}
