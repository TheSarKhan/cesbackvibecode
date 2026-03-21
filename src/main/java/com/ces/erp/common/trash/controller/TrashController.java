package com.ces.erp.common.trash.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.trash.dto.TrashItem;
import com.ces.erp.common.trash.service.TrashService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trash")
@RequiredArgsConstructor
@Tag(name = "Trash", description = "Silinmiş məlumatlar")
public class TrashController {

    private final TrashService trashService;

    @GetMapping
    @PreAuthorize("hasAuthority('TRASH:GET')")
    @Operation(summary = "Silinmiş məlumatları gətir")
    public ResponseEntity<ApiResponse<List<TrashItem>>> getAll(
            @RequestParam(required = false) String module) {
        return ResponseEntity.ok(ApiResponse.success(trashService.getAll(module)));
    }

    @PostMapping("/{entityType}/{id}/restore")
    @PreAuthorize("hasAuthority('TRASH:PUT')")
    @Operation(summary = "Silinmiş məlumatı bərpa et")
    public ResponseEntity<ApiResponse<Void>> restore(
            @PathVariable String entityType,
            @PathVariable Long id) {
        trashService.restore(entityType, id);
        return ResponseEntity.ok(ApiResponse.ok("Məlumat bərpa edildi"));
    }
}
