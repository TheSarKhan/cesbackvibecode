package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.LeaveStatus;
import com.ces.erp.hr.dto.LeaveRequestDto;
import com.ces.erp.hr.dto.LeaveRequestResponse;
import com.ces.erp.hr.service.LeaveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/leaves")
@RequiredArgsConstructor
@Tag(name = "HR — Məzuniyyət", description = "Məzuniyyət tələbləri və təsdiq")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveRequestResponse>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getPaged(page, size, employeeId, status)));
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getByEmployee(employeeId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(@Valid @RequestBody LeaveRequestDto req) {
        return ResponseEntity.ok(ApiResponse.success("Məzuniyyət tələbi yaradıldı", leaveService.create(req)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(@PathVariable Long id,
                                                                     @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.success("Məzuniyyət təsdiqləndi", leaveService.approve(id, note)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(@PathVariable Long id,
                                                                    @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(ApiResponse.success("Məzuniyyət rədd edildi", leaveService.reject(id, note)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Məzuniyyət ləğv edildi", leaveService.cancel(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        leaveService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tələb silindi"));
    }
}
