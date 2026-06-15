package com.ces.erp.hr.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.hr.dto.AttendanceRequest;
import com.ces.erp.hr.dto.AttendanceResponse;
import com.ces.erp.hr.entity.AttendanceRecord;
import com.ces.erp.hr.entity.Employee;
import com.ces.erp.hr.repository.AttendanceRecordRepository;
import com.ces.erp.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService implements ApprovalHandler {

    private final AttendanceRecordRepository repo;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Override public String getEntityType() { return "ATTENDANCE"; }
    @Override public String getModuleCode()  { return "HR"; }
    @Override public String getLabel(Long id) {
        AttendanceRecord rec = loadActive(id);
        Employee emp = rec.getEmployee();
        return (emp != null ? emp.getFullName() : "Davamiyyət") + " — " + rec.getDate();
    }
    @Override public Object getSnapshot(Long id) { return AttendanceResponse.from(loadActive(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        // upsert tip endpoint olduğu üçün ayrıca edit dəstəklənmir.
        throw new BusinessException("Davamiyyət üçün ayrıca edit dəstəklənmir");
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    private AttendanceRecord loadActive(Long id) {
        return repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Davamiyyət qeydi", id));
    }

    public List<AttendanceResponse> getByEmployee(Long employeeId, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end == null) end = LocalDate.now();
        return repo.findAllByEmployeeIdAndDateBetweenAndDeletedFalseOrderByDateAsc(employeeId, start, end).stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    public List<AttendanceResponse> getByDateRange(LocalDate start, LocalDate end) {
        return repo.findAllByDateBetweenAndDeletedFalseOrderByDateAsc(start, end).stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional
    public AttendanceResponse upsert(AttendanceRequest req) {
        Employee emp = employeeRepository.findByIdAndDeletedFalse(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("İşçi", req.getEmployeeId()));

        AttendanceRecord rec = repo.findByEmployeeIdAndDateAndDeletedFalse(req.getEmployeeId(), req.getDate())
                .orElse(AttendanceRecord.builder()
                        .employee(emp)
                        .date(req.getDate())
                        .build());

        rec.setStatus(req.getStatus());
        rec.setHoursWorked(req.getHoursWorked() != null ? req.getHoursWorked() : new BigDecimal("8.00"));
        rec.setNotes(req.getNotes());

        AttendanceRecord saved = repo.save(rec);
        auditService.log("HR_DAVAMİYYƏT", saved.getId(), emp.getFullName() + " " + req.getDate(),
                "QEYDƏ_ALINDI", "Davamiyyət yeniləndi");
        return AttendanceResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "ATTENDANCE", isDelete = true)
    public void delete(Long id) {
        AttendanceRecord rec = loadActive(id);
        rec.softDelete();
        repo.save(rec);
    }
}
