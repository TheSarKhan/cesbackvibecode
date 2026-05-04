package com.ces.erp.hr.service;

import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.LeaveStatus;
import com.ces.erp.hr.dto.HrDashboardResponse;
import com.ces.erp.hr.entity.PayrollPeriod;
import com.ces.erp.hr.repository.EmployeeRepository;
import com.ces.erp.hr.repository.LeaveRequestRepository;
import com.ces.erp.hr.repository.PayrollPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HrDashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PayrollPeriodRepository periodRepository;

    private static final String[] AZ_MONTHS = {
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
        "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"
    };

    public HrDashboardResponse getStats() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        long total = employeeRepository.countByDeletedFalse();
        long active = employeeRepository.countByStatusAndDeletedFalse(EmployeeStatus.ACTIVE);
        long onLeave = employeeRepository.countByStatusAndDeletedFalse(EmployeeStatus.ON_LEAVE);
        long pendingLeaves = leaveRepository.countByStatusAndDeletedFalse(LeaveStatus.PENDING);

        var period = periodRepository.findByYearAndMonthAndDeletedFalse(year, month).orElse(null);
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal companyCost = BigDecimal.ZERO;
        Integer entryCount = 0;
        String status = null;
        Long periodId = null;
        if (period != null) {
            gross = nz(period.getTotalGross());
            net = nz(period.getTotalNet());
            companyCost = nz(period.getTotalGross()).add(nz(period.getTotalEmployerContributions()));
            status = period.getStatus().name();
            periodId = period.getId();
            entryCount = period.getEntries() == null ? 0
                    : (int) period.getEntries().stream().filter(e -> !e.isDeleted()).count();
        }

        return HrDashboardResponse.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .onLeaveEmployees(onLeave)
                .pendingLeaveRequests(pendingLeaves)
                .currentMonthGross(gross)
                .currentMonthNet(net)
                .currentMonthCompanyCost(companyCost)
                .currentMonthEntryCount(entryCount)
                .currentMonthLabel(AZ_MONTHS[month - 1] + " " + year)
                .currentMonthStatus(status)
                .currentMonthPeriodId(periodId)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
