package com.ces.erp.auth.controller;

import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.LoginResponse;
import com.ces.erp.auth.dto.RefreshTokenRequest;
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
}
