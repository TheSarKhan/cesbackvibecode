package com.ces.erp.bank.controller;

import com.ces.erp.bank.dto.BankRequest;
import com.ces.erp.bank.dto.BankResponse;
import com.ces.erp.bank.service.BankService;
import com.ces.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
@Tag(name = "Banks", description = "Bank məlumatları")
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(bankService.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG:POST')")
    public ResponseEntity<ApiResponse<BankResponse>> create(@Valid @RequestBody BankRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bank yaradıldı", bankService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:PUT')")
    public ResponseEntity<ApiResponse<BankResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BankRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bank yeniləndi", bankService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bankService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Bank silindi"));
    }
}
