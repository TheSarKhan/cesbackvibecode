package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PeriodComparisonResponse {
    private String currentPeriodLabel;
    private String previousPeriodLabel;
    private ReportSummaryResponse currentPeriod;
    private ReportSummaryResponse previousPeriod;
}
