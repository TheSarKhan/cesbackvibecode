package com.ces.erp.accounting.dto.report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CashFlowItem {
    private String month;
    private Integer monthNum;
    private Integer year;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal net;
}
