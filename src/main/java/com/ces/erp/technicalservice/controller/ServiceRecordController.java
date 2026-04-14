package com.ces.erp.technicalservice.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.technicalservice.dto.ServiceRecordRequest;
import com.ces.erp.technicalservice.dto.ServiceRecordResponse;
import com.ces.erp.technicalservice.entity.ServiceRecordType;
import com.ces.erp.technicalservice.service.ServiceRecordService;
import com.ces.erp.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service/records")
@RequiredArgsConstructor
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;

    @GetMapping
    public ApiResponse<List<ServiceRecordResponse>> getAll(
            @RequestParam(required = false) ServiceRecordType type) {
        return ApiResponse.success(serviceRecordService.getAll(type));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceRecordResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(serviceRecordService.getById(id));
    }

    @PostMapping
    public ApiResponse<ServiceRecordResponse> create(
            @RequestBody ServiceRecordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(serviceRecordService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceRecordResponse> update(
            @PathVariable Long id,
            @RequestBody ServiceRecordRequest request) {
        return ApiResponse.success(serviceRecordService.update(id, request));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<ServiceRecordResponse> complete(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        EquipmentStatus statusAfter = EquipmentStatus.valueOf(body.get("statusAfter"));
        BigDecimal cost = body.get("cost") != null && !body.get("cost").isBlank()
                ? new BigDecimal(body.get("cost")) : null;
        return ApiResponse.success(serviceRecordService.complete(id, statusAfter, cost, principal.getId()));
    }

    @PatchMapping("/{id}/checklist/{itemId}")
    public ApiResponse<ServiceRecordResponse> updateChecklistItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body) {
        boolean checked = Boolean.TRUE.equals(body.get("checked"));
        String note = body.get("note") != null ? body.get("note").toString() : null;
        return ApiResponse.success(serviceRecordService.updateChecklistItem(id, itemId, checked, note));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceRecordService.delete(id);
        return ApiResponse.success(null);
    }
}
