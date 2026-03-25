package com.ces.erp.auth.controller;

import com.ces.erp.auth.dto.ForgotPasswordRequest;
import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.LoginResponse;
import com.ces.erp.auth.dto.RefreshTokenRequest;
import com.ces.erp.auth.dto.ResetPasswordRequest;
import com.ces.erp.auth.service.AuthService;
import com.ces.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Giriş, token yenilənməsi, çıxış")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Sistemə giriş")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access token yenilə")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sistemdən çıxış")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Uğurla çıxış edildi"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Şifrəmi unutdum — email göndər")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Əgər bu email mövcuddursa, şifrə yeniləmə linki göndərildi"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Şifrəni yenilə")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Şifrə uğurla yeniləndi"));
    }
}
