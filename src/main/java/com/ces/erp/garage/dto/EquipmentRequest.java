package com.ces.erp.garage.dto;

import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OwnershipType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentRequest {

    @NotBlank(message = "Texnika kodu boş ola bilməz")
    @Size(min = 2, max = 50, message = "Texnika kodu 2-50 simvol arasında olmalıdır")
    @Pattern(regexp = ".*[\\p{L}\\d].*", message = "Texnika kodu ən azı bir hərf və ya rəqəm içərməlidir")
    private String equipmentCode;

    @NotBlank(message = "Ad boş ola bilməz")
    @Size(min = 2, max = 200, message = "Ad 2-200 simvol arasında olmalıdır")
    @Pattern(regexp = ".*\\p{L}.*", message = "Ad ən azı bir hərf içərməlidir")
    private String name;

    @NotBlank(message = "Növ boş ola bilməz")
    @Size(min = 2, max = 100, message = "Növ 2-100 simvol arasında olmalıdır")
    @Pattern(regexp = ".*\\p{L}.*", message = "Növ ən azı bir hərf içərməlidir")
    private String type;

    @Size(max = 100, message = "Seriya nömrəsi maksimum 100 simvol ola bilər")
    private String serialNumber;

    @Size(max = 100, message = "Brend maksimum 100 simvol ola bilər")
    private String brand;

    @Size(max = 100, message = "Model maksimum 100 simvol ola bilər")
    private String model;

    @Min(value = 1900, message = "İstehsal ili 1900-dən kiçik ola bilməz")
    @Max(value = 2099, message = "İstehsal ili 2099-dan böyük ola bilməz")
    private Integer manufactureYear;

    @NotNull(message = "Alınma tarixi tələb olunur")
    private LocalDate purchaseDate;

    @NotNull(message = "Alış qiyməti tələb olunur")
    @DecimalMin(value = "0", message = "Alış qiyməti mənfi ola bilməz")
    @Digits(integer = 12, fraction = 2, message = "Alış qiyməti düzgün formatda deyil")
    private BigDecimal purchasePrice;

    @Size(max = 20, message = "Qeydiyyat nişanı maksimum 20 simvol ola bilər")
    private String plateNumber;

    @DecimalMin(value = "0", message = "Çəki mənfi ola bilməz")
    private BigDecimal weightTon;

    @DecimalMin(value = "0", message = "Bazar dəyəri mənfi ola bilməz")
    @Digits(integer = 12, fraction = 2, message = "Bazar dəyəri düzgün formatda deyil")
    private BigDecimal currentMarketValue;

    @DecimalMin(value = "0", message = "Amortizasiya mənfi ola bilməz")
    @DecimalMax(value = "100", message = "Amortizasiya 100%-dən çox ola bilməz")
    private BigDecimal depreciationRate;

    @DecimalMin(value = "0", message = "Saat/KM göstəricisi mənfi ola bilməz")
    private BigDecimal hourKmCounter;

    @DecimalMin(value = "0", message = "Moto saatlar mənfi ola bilməz")
    private BigDecimal motoHours;

    @Size(max = 200, message = "Saxlanma yeri maksimum 200 simvol ola bilər")
    private String storageLocation;

    private Long responsibleUserId;

    @NotNull(message = "Mülkiyyət növü boş ola bilməz")
    private OwnershipType ownershipType;

    // CONTRACTOR üçün
    private Long ownerContractorId;

    // INVESTOR üçün
    @Size(max = 200, message = "İnvestor adı maksimum 200 simvol ola bilər")
    private String ownerInvestorName;

    @Pattern(regexp = "^$|^\\d{10}$", message = "VÖEN 10 rəqəmdən ibarət olmalıdır")
    private String ownerInvestorVoen;

    @Pattern(regexp = "^$|^(\\+994|0)(10|12|50|51|55|60|70|77|99)\\d{7}$", message = "Düzgün telefon nömrəsi daxil edin")
    private String ownerInvestorPhone;

    private LocalDate lastInspectionDate;
    private LocalDate nextInspectionDate;

    @Size(max = 100, message = "Texniki hazırlıq statusu maksimum 100 simvol ola bilər")
    private String technicalReadinessStatus;

    @NotNull(message = "Status boş ola bilməz")
    private EquipmentStatus status;

    @Size(max = 100, message = "Təmir statusu maksimum 100 simvol ola bilər")
    private String repairStatus;

    @Size(max = 1000, message = "Qeyd maksimum 1000 simvol ola bilər")
    private String notes;

    private List<Long> safetyEquipmentIds;
}
