package com.ces.erp.user.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.user.dto.UserApprovalRequest;
import com.ces.erp.user.dto.UserRequest;
import com.ces.erp.user.dto.UserResponse;
import com.ces.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "İstifadəçi idarəetməsi")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Bütün istifadəçiləri gətir")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "İstifadəçiləri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllPaged(page, size, q, departmentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "İstifadəçini ID ilə gətir")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:POST')")
    @Operation(summary = "Yeni istifadəçi yarat")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İstifadəçi yaradıldı", userService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "İstifadəçini yenilə")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İstifadəçi yeniləndi", userService.update(id, request)));
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "Approval icazəsini təyin et / dəyişdir")
    public ResponseEntity<ApiResponse<UserResponse>> updateApproval(@PathVariable Long id,
                                                                      @RequestBody UserApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Approval yeniləndi", userService.updateApproval(id, request)));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "İstifadəçini aktiv/passiv et")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id) {
        userService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok("Status dəyişdirildi"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:DELETE')")
    @Operation(summary = "İstifadəçini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("İstifadəçi silindi"));
    }
}
