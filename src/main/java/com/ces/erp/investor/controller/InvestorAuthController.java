package com.ces.erp.investor.controller;

import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.RefreshTokenRequest;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.investor.dto.InvestorLoginResponse;
import com.ces.erp.investor.service.InvestorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Portal (mobil investor tətbiqi) kimlik doğrulaması — internal auth-dan ayrı, permitAll. */
@RestController
@RequestMapping("/api/investor-auth")
@RequiredArgsConstructor
@Tag(name = "Investor Auth", description = "İnvestor portal girişi")
public class InvestorAuthController {

    private final InvestorAuthService investorAuthService;

    @PostMapping("/login")
    @Operation(summary = "İnvestor portal girişi (email + şifrə)")
    public ResponseEntity<ApiResponse<InvestorLoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Giriş uğurludur", investorAuthService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "İnvestor token yeniləmə")
    public ResponseEntity<ApiResponse<InvestorLoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(investorAuthService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "İnvestor çıxışı")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        investorAuthService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Çıxış edildi"));
    }
}
