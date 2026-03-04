package com.ces.erp.systemmodule.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.systemmodule.dto.SystemModuleResponse;
import com.ces.erp.systemmodule.service.SystemModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Tag(name = "System Modules", description = "Sistem modullarının siyahısı (rol yaradılarkən istifadə edilir)")
public class SystemModuleController {

    private final SystemModuleService systemModuleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Bütün sistem modullarını gətir")
    public ResponseEntity<ApiResponse<List<SystemModuleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(systemModuleService.getAll()));
    }
}
