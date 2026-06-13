package com.ces.erp.user.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.user.dto.UserApprovalRequest;
import com.ces.erp.user.dto.UserContactRequest;
import com.ces.erp.user.dto.UserPasswordRequest;
import com.ces.erp.user.dto.UserRequest;
import com.ces.erp.user.dto.UserResponse;
import com.ces.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "İstifadəçi idarəetməsi")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cari istifadəçinin profili və güncəl icazələri")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(principal.getUsername())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:GET')")
    @Operation(summary = "Bütün istifadəçiləri gətir")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll()));
    }

    // ───── Self-service (cari istifadəçi) ──────────────────

    @GetMapping("/me")
    @Operation(summary = "Cari istifadəçi profili")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrent(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrent(principal.getId())));
    }

    @PutMapping("/me/contact")
    @Operation(summary = "Cari istifadəçi əlaqə məlumatlarını yenilə")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Əlaqə məlumatları yeniləndi",
                userService.updateMyContact(principal.getId(), request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cari istifadəçi şifrəsini dəyiş")
    public ResponseEntity<ApiResponse<Void>> updateMyPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserPasswordRequest request) {
        userService.updateMyPassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Şifrə yeniləndi"));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:GET')")
    @Operation(summary = "İstifadəçiləri səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllPaged(page, size, q, departmentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:GET')")
    @Operation(summary = "İstifadəçini ID ilə gətir")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:POST')")
    @Operation(summary = "Yeni istifadəçi yarat")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İstifadəçi yaradıldı", userService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:PUT')")
    @Operation(summary = "İstifadəçini yenilə")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("İstifadəçi yeniləndi", userService.update(id, request)));
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:PUT')")
    @Operation(summary = "Approval icazəsini təyin et / dəyişdir")
    public ResponseEntity<ApiResponse<UserResponse>> updateApproval(@PathVariable Long id,
                                                                      @RequestBody UserApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Approval yeniləndi", userService.updateApproval(id, request)));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:PUT')")
    @Operation(summary = "İstifadəçini aktiv/passiv et")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id) {
        userService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.ok("Status dəyişdirildi"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_MANAGEMENT:DELETE')")
    @Operation(summary = "İstifadəçini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("İstifadəçi silindi"));
    }
}
