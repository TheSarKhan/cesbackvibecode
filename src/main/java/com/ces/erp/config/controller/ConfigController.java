package com.ces.erp.config.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.config.dto.ConfigItemRequest;
import com.ces.erp.config.dto.ConfigItemResponse;
import com.ces.erp.config.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "Configuration", description = "Kontent konfiqurasiyası")
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Bütün konfiqurasiya elementlərini kateqoriya üzrə gətir")
    public ResponseEntity<ApiResponse<Map<String, List<ConfigItemResponse>>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(configService.getAllGrouped()));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Konfiqurasiya elementlərini səhifələnmiş gətir")
    public ResponseEntity<ApiResponse<PagedResponse<ConfigItemResponse>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(configService.getAllPaged(page, size, q, category)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Kateqoriya siyahısını gətir")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(configService.getCategories()));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Kateqoriyaya görə elementləri gətir")
    public ResponseEntity<ApiResponse<List<ConfigItemResponse>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(configService.getByCategory(category)));
    }

    @GetMapping("/category/{category}/active")
    @Operation(summary = "Kateqoriyaya görə aktiv elementləri gətir (dropdown-lar üçün)")
    public ResponseEntity<ApiResponse<List<ConfigItemResponse>>> getActiveByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(configService.getActiveByCategory(category)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:GET')")
    @Operation(summary = "Konfiqurasiya elementini ID ilə gətir")
    public ResponseEntity<ApiResponse<ConfigItemResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(configService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG:POST')")
    @Operation(summary = "Yeni konfiqurasiya elementi yarat")
    public ResponseEntity<ApiResponse<ConfigItemResponse>> create(@Valid @RequestBody ConfigItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Element yaradıldı", configService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:PUT')")
    @Operation(summary = "Konfiqurasiya elementini yenilə")
    public ResponseEntity<ApiResponse<ConfigItemResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ConfigItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Element yeniləndi", configService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:DELETE')")
    @Operation(summary = "Konfiqurasiya elementini sil")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Element silindi"));
    }

    @PostMapping("/category/{category}/reorder")
    @PreAuthorize("hasAuthority('CONFIG:PUT')")
    @Operation(summary = "Kateqoriya daxilində sıralanı dəyiş")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @PathVariable String category, @RequestBody List<Long> orderedIds) {
        configService.reorder(category, orderedIds);
        return ResponseEntity.ok(ApiResponse.ok("Sıralama yeniləndi"));
    }
}
