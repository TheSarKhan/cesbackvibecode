package com.ces.erp.permission.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.permission.dto.PermissionResponse;
import com.ces.erp.permission.dto.PermissionUpdateRequest;
import com.ces.erp.permission.entity.Permission;
import com.ces.erp.permission.repository.PermissionRepository;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dinamik icazə kataloqu — rol redaktə ekranı bunu modul üzrə qruplaşdırıb göstərir.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Dinamik icazə kataloqu")
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final SystemModuleRepository moduleRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:GET')")
    @Operation(summary = "Bütün icazələr (modul üzrə sıralanmış)")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAll() {
        Map<String, String> nameByCode = moduleRepository.findAll().stream()
                .collect(Collectors.toMap(m -> m.getCode(), m -> m.getNameAz(), (a, b) -> a));
        List<PermissionResponse> result = permissionRepository.findAllByOrderByModuleCodeAscActionAsc().stream()
                .map(p -> PermissionResponse.from(p, nameByCode.get(p.getModuleCode())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION:PUT')")
    @Operation(summary = "İcazə etiketini redaktə et")
    @Transactional
    public ResponseEntity<ApiResponse<PermissionResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody PermissionUpdateRequest req) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İcazə", id));
        p.setLabelAz(req.getLabelAz());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        Permission saved = permissionRepository.save(p);
        String nameAz = moduleRepository.findByCode(saved.getModuleCode()).map(m -> m.getNameAz()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("İcazə yeniləndi", PermissionResponse.from(saved, nameAz)));
    }
}
