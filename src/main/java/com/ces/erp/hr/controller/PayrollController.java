package com.ces.erp.hr.controller;

import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.enums.PayrollStatus;
import com.ces.erp.hr.dto.PayrollEntryRequest;
import com.ces.erp.hr.dto.PayrollEntryResponse;
import com.ces.erp.hr.dto.PayrollPeriodRequest;
import com.ces.erp.hr.dto.PayrollPeriodResponse;
import com.ces.erp.hr.service.PayrollPdfService;
import com.ces.erp.hr.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/payroll")
@RequiredArgsConstructor
@Tag(name = "HR — Əməkhaqqı", description = "Aylıq əməkhaqqı dövrləri və hesablamalar")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollPdfService pdfService;

    // ─── Periods ──
    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<List<PayrollPeriodResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getAll()));
    }

    @GetMapping("/periods/paged")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollPeriodResponse>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PayrollStatus status) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPaged(page, size, year, status)));
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getById(id)));
    }

    @PostMapping("/periods")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Yeni aylıq dövr yarat")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> create(
            @Valid @RequestBody PayrollPeriodRequest req,
            @RequestParam(defaultValue = "true") boolean autoPopulate) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dövr yaradıldı", payrollService.createPeriod(req, autoPopulate)));
    }

    @PutMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody PayrollPeriodRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Dövr yeniləndi", payrollService.updatePeriod(id, req)));
    }

    @PostMapping("/periods/{id}/populate")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    @Operation(summary = "Dövrü bütün aktiv işçilərlə doldur (mövcud entry-lər saxlanılır)")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> populate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Dövr dolduruldu", payrollService.populate(id)));
    }

    @PatchMapping("/periods/{id}/approve")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Dövr təsdiqləndi", payrollService.approve(id)));
    }

    @PatchMapping("/periods/{id}/mark-paid")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Dövr ödənmiş kimi qeyd olundu", payrollService.markPaid(id)));
    }

    @PatchMapping("/periods/{id}/reopen")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Dövr yenidən açıldı", payrollService.reopen(id)));
    }

    @DeleteMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        payrollService.deletePeriod(id);
        return ResponseEntity.ok(ApiResponse.ok("Dövr silindi"));
    }

    // ─── Entries ──
    @PostMapping("/periods/{periodId}/entries/{employeeId}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:POST')")
    public ResponseEntity<ApiResponse<PayrollEntryResponse>> addEntry(@PathVariable Long periodId,
                                                                      @PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Sətir əlavə edildi",
                payrollService.addEntryForEmployee(periodId, employeeId)));
    }

    @GetMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<PayrollEntryResponse>> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getEntry(id)));
    }

    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:PUT')")
    public ResponseEntity<ApiResponse<PayrollEntryResponse>> updateEntry(@PathVariable Long id,
                                                                         @RequestBody PayrollEntryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Sətir yeniləndi", payrollService.updateEntry(id, req)));
    }

    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        payrollService.removeEntry(id);
        return ResponseEntity.ok(ApiResponse.ok("Sətir silindi"));
    }

    @GetMapping("/employees/{employeeId}/entries")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    public ResponseEntity<ApiResponse<List<PayrollEntryResponse>>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getEntriesByEmployee(employeeId)));
    }

    // ─── PDF Export ──
    @GetMapping("/periods/{id}/pdf")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "Aylıq əməkhaqqı cədvəlini PDF olaraq endir")
    public ResponseEntity<byte[]> downloadPeriodPdf(@PathVariable Long id) {
        var period = payrollService.getById(id);
        byte[] pdf = pdfService.generatePeriodReport(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payroll-" + period.getYear() + "-" + period.getMonth() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/entries/{id}/payslip")
    @PreAuthorize("hasAuthority('HR_MANAGEMENT:GET')")
    @Operation(summary = "İşçi üçün aylıq pay slip PDF")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long id) {
        byte[] pdf = pdfService.generatePayslip(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .body(pdf);
    }
}
