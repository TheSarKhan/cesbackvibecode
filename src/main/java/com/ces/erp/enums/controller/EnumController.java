package com.ces.erp.enums.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.enums.*;
import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bütün {@link LabeledEnum} enum-larının kod→etiket xəritəsini tək endpoint-lə paylayır.
 * Frontend bunu bir dəfə çəkib cache-ləyir və mərkəzi {@code enumLabel} helper-i ilə işlədir.
 * Yeni etiketli enum əlavə olunduqda yalnız aşağıdakı registry-yə bir sətir əlavə edilir.
 */
@RestController
@RequestMapping("/api/enums")
@Tag(name = "Enums", description = "Enum kod→etiket xəritəsi (tək doğru mənbə)")
public class EnumController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bütün enum-ların kod+etiket siyahısı")
    public ResponseEntity<ApiResponse<Map<String, List<EnumOptionDto>>>> getAll() {
        Map<String, List<EnumOptionDto>> result = new LinkedHashMap<>();

        register(result, "RequestStatus", RequestStatus.values());
        register(result, "EquipmentStatus", EquipmentStatus.values());
        register(result, "CustomerStatus", CustomerStatus.values());
        register(result, "ContractorStatus", ContractorStatus.values());
        register(result, "InvoiceStatus", InvoiceStatus.values());
        register(result, "InvoiceType", InvoiceType.values());
        register(result, "DocumentType", DocumentType.values());
        register(result, "ProjectStatus", ProjectStatus.values());
        register(result, "ProjectType", ProjectType.values());
        register(result, "PayrollStatus", PayrollStatus.values());
        register(result, "PayableStatus", PayableStatus.values());
        register(result, "ReceivableStatus", ReceivableStatus.values());
        register(result, "RecurrenceFrequency", RecurrenceFrequency.values());
        register(result, "AttendanceStatus", AttendanceStatus.values());
        register(result, "LeaveStatus", LeaveStatus.values());
        register(result, "LeaveType", LeaveType.values());
        register(result, "AdjustmentType", AdjustmentType.values());
        register(result, "EmployeeStatus", EmployeeStatus.values());
        register(result, "Gender", Gender.values());
        register(result, "OperatorDocumentType", OperatorDocumentType.values());
        register(result, "OwnershipType", OwnershipType.values());
        register(result, "RiskLevel", RiskLevel.values());
        register(result, "OperationType", OperationType.values());
        register(result, "OperationStatus", OperationStatus.values());
        register(result, "DeductionAppliesTo", DeductionAppliesTo.values());
        register(result, "DeductionParty", DeductionParty.values());
        register(result, "PartyType", PartyType.values());
        register(result, "ServiceRecordType", ServiceRecordType.values());
        register(result, "RequestDocumentType", RequestDocumentType.values());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void register(Map<String, List<EnumOptionDto>> map, String name, LabeledEnum[] values) {
        List<EnumOptionDto> options = new ArrayList<>(values.length);
        for (LabeledEnum v : values) {
            options.add(new EnumOptionDto(v.getCode(), v.getLabel()));
        }
        map.put(name, options);
    }
}
