package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.hr.dto.AttendanceRequest;
import com.ces.erp.hr.dto.AttendanceResponse;
import com.ces.erp.hr.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hr/attendance")
@RequiredArgsConstructor
@Tag(name = "HR — Davamiyyət", description = "İşçi davamiyyətinin idarə edilməsi")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getByEmployee(employeeId, start, end)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getByDateRange(start, end)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> upsert(@Valid @RequestBody AttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Davamiyyət qeyd edildi", attendanceService.upsert(req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Qeyd silindi"));
    }
}
