package com.ces.erp.department.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.department.dto.DepartmentRequest;
import com.ces.erp.department.dto.DepartmentResponse;
import com.ces.erp.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Şöbə idarəetməsi")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Bütün şöbələri gətir")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Şöbəni ID ilə gətir")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:POST')")
    @Operation(summary = "Yeni şöbə yarat")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Şöbə yaradıldı", departmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "Şöbəni yenilə")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Şöbə yeniləndi", departmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:DELETE')")
    @Operation(summary = "Şöbəni sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Şöbə silindi"));
    }
}
