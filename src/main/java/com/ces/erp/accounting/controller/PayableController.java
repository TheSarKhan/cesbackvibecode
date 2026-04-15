package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.AddPayablePaymentRequest;
import com.ces.erp.accounting.dto.PayablePaymentResponse;
import com.ces.erp.accounting.dto.PayableResponse;
import com.ces.erp.accounting.service.PayableService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.enums.PayableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting/payables")
@RequiredArgsConstructor
public class PayableController {

    private final PayableService payableService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PayableResponse>>> getPayables(
            @RequestParam(required = false) PayableStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(payableService.getPayables(status, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PayableResponse>> getPayable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payableService.getPayable(id)));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<PayablePaymentResponse>> addPayment(
            @PathVariable Long id, @RequestBody AddPayablePaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(payableService.addPayment(id, req)));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id, @PathVariable Long paymentId) {
        payableService.deletePayment(id, paymentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PayableResponse>> completePayable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payableService.complete(id)));
    }
}
