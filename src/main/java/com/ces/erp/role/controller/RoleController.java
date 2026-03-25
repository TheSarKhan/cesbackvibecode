package com.ces.erp.role.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.role.dto.RoleRequest;
import com.ces.erp.role.dto.RoleResponse;
import com.ces.erp.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Rol və icazə idarəetməsi")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Bütün rolları gətir")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Rolları səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<RoleResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllPaged(page, size, q, departmentId)));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Şöbəyə aid rolları gətir")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getByDepartment(departmentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Rolu ID ilə gətir (icazələrlə birlikdə)")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:POST')")
    @Operation(summary = "Yeni rol yarat")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rol yaradıldı", roleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "Rolu yenilə (icazələrlə birlikdə)")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rol yeniləndi", roleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:DELETE')")
    @Operation(summary = "Rolu sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol silindi"));
    }
}
