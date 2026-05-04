package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.hr.dto.HrDashboardResponse;
import com.ces.erp.hr.service.HrDashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/dashboard")
@RequiredArgsConstructor
@Tag(name = "HR — Dashboard", description = "İnsan Resursları statistikaları")
public class HrDashboardController {

    private final HrDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<HrDashboardResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
    }
}
