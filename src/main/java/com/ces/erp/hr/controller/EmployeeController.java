package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.hr.dto.EmployeeRequest;
import com.ces.erp.hr.dto.EmployeeResponse;
import com.ces.erp.hr.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
@Tag(name = "HR — İşçilər", description = "İnsan Resursları: İşçi idarəetməsi")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Bütün işçiləri gətir")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "İşçiləri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeService.getPaged(page, size, q, status, departmentId, positionId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "İşçini ID ilə gətir")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni işçi əlavə et")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.success("İşçi əlavə edildi", employeeService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "İşçi məlumatlarını yenilə")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.success("İşçi yeniləndi", employeeService.update(id, req)));
    }

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    @Operation(summary = "İşçini işdən çıxar")
    public ResponseEntity<ApiResponse<EmployeeResponse>> terminate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        LocalDate date = body.get("terminationDate") != null
                ? LocalDate.parse(body.get("terminationDate").toString()) : LocalDate.now();
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(ApiResponse.success("İşçi işdən çıxarıldı", employeeService.terminate(id, date, reason)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    @Operation(summary = "İşçini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("İşçi silindi"));
    }
}
