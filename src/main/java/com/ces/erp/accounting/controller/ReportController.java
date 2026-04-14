package com.ces.erp.accounting.controller;

import com.ces.erp.accounting.dto.report.*;
import com.ces.erp.accounting.service.ReportService;
import com.ces.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounting/reports")
@RequiredArgsConstructor
@Tag(name = "Accounting Reports", description = "Mühasibatlıq bölməsi üçün hesabatlar")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Maliyyə xülasəsi")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(startDate, endDate)));
    }

    @GetMapping("/monthly-trend")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Aylıq trend hesabatı")
    public ResponseEntity<ApiResponse<List<MonthlyTrendItem>>> getMonthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyTrend(startDate, endDate)));
    }

    @GetMapping("/by-project")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Layihələr üzrə hesabat")
    public ResponseEntity<ApiResponse<List<ProjectReportItem>>> getProjectReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProjectReport(startDate, endDate)));
    }

    @GetMapping("/by-partner")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Tərəfdaşlar (Podratçı / İnvestor) üzrə hesabat")
    public ResponseEntity<ApiResponse<List<PartnerReportItem>>> getPartnerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPartnerReport(startDate, endDate)));
    }

    @GetMapping("/expense-breakdown")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Xərc təsnifatı")
    public ResponseEntity<ApiResponse<List<ExpenseBreakdownItem>>> getExpenseBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getExpenseBreakdown(startDate, endDate)));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Cash Flow hesabatı")
    public ResponseEntity<ApiResponse<List<CashFlowItem>>> getCashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getCashFlow(startDate, endDate)));
    }

    @GetMapping("/comparison")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Dövr müqayisəsi")
    public ResponseEntity<ApiResponse<PeriodComparisonResponse>> getComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentEnd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prevStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prevEnd) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getComparison(currentStart, currentEnd, prevStart, prevEnd)));
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('ACCOUNTING:GET')")
    @Operation(summary = "Debitorlar hesabatı")
    public ResponseEntity<ApiResponse<List<ReceivableReportItem>>> getReceivableReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReceivableReport(startDate, endDate)));
    }
}
