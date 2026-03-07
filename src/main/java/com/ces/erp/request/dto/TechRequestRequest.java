package com.ces.erp.request.dto;

import com.ces.erp.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TechRequestRequest {

    private Long customerId;

    @NotBlank(message = "Şirkət adı tələb olunur")
    private String companyName;

    private String contactPerson;
    private String contactPhone;
    private String projectName;
    private String region;
    private LocalDate requestDate;
    private ProjectType projectType;
    private Integer dayCount;

    private boolean transportationRequired = false;

    private List<ParamDto> params = new ArrayList<>();

    private Long selectedEquipmentId;
    private String notes;

    @Data
    public static class ParamDto {
        private String paramKey;
        private String paramValue;
    }
}
