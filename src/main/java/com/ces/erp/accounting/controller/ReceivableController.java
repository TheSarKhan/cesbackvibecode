package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.AddPaymentRequest;
import com.ces.erp.accounting.dto.ReceivablePaymentResponse;
import com.ces.erp.accounting.dto.ReceivableResponse;
import com.ces.erp.accounting.service.ReceivableService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.enums.ReceivableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReceivableResponse>>> getReceivables(
            @RequestParam(required = false) ReceivableStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(receivableService.getReceivables(status, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceivableResponse>> getReceivable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(receivableService.getReceivable(id)));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<ReceivablePaymentResponse>> addPayment(
            @PathVariable Long id, @RequestBody AddPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(receivableService.addPayment(id, req)));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id, @PathVariable Long paymentId) {
        receivableService.deletePayment(id, paymentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ReceivableResponse>> completeReceivable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(receivableService.complete(id)));
    }
}
