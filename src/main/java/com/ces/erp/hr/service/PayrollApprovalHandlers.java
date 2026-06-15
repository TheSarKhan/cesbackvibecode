package com.ces.erp.hr.service;

import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.hr.dto.PayrollEntryRequest;
import com.ces.erp.hr.dto.PayrollEntryResponse;
import com.ces.erp.hr.dto.PayrollPeriodRequest;
import com.ces.erp.hr.dto.PayrollPeriodResponse;
import com.ces.erp.hr.entity.PayrollEntry;
import com.ces.erp.hr.entity.PayrollPeriod;
import com.ces.erp.hr.repository.PayrollEntryRepository;
import com.ces.erp.hr.repository.PayrollPeriodRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PayrollService həm dövr (period), həm də sətir (entry) idarə etdiyi üçün
 * iki ayrı ApprovalHandler bean-i ilə təsdiq sisteminə qoşulur.
 */
public class PayrollApprovalHandlers {

    @Component
    @RequiredArgsConstructor
    public static class PeriodHandler implements ApprovalHandler {
        private final PayrollService service;
        private final PayrollPeriodRepository periodRepo;
        private final ObjectMapper objectMapper;

        @Override public String getEntityType() { return "PAYROLL_PERIOD"; }
        @Override public String getModuleCode()  { return "HR"; }
        @Override public String getLabel(Long id) {
            PayrollPeriod p = loadPeriod(id);
            return p.getMonth() + "/" + p.getYear();
        }
        @Override public Object getSnapshot(Long id) { return PayrollPeriodResponse.from(loadPeriod(id), false); }

        @Override
        public void applyEdit(Long id, String json) {
            try {
                PayrollPeriodRequest req = objectMapper.readValue(json, PayrollPeriodRequest.class);
                ApprovalContext.setApplying(true);
                try { service.updatePeriod(id, req); } finally { ApprovalContext.clear(); }
            } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
        }

        @Override
        public void applyDelete(Long id) {
            ApprovalContext.setApplying(true);
            try { service.deletePeriod(id); } finally { ApprovalContext.clear(); }
        }

        private PayrollPeriod loadPeriod(Long id) {
            return periodRepo.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payroll period", id));
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class EntryHandler implements ApprovalHandler {
        private final PayrollService service;
        private final PayrollEntryRepository entryRepo;
        private final ObjectMapper objectMapper;

        @Override public String getEntityType() { return "PAYROLL_ENTRY"; }
        @Override public String getModuleCode()  { return "HR"; }
        @Override public String getLabel(Long id) {
            PayrollEntry e = loadEntry(id);
            return e.getEmployeeFullName() + " — " + (e.getPeriod() != null ? (e.getPeriod().getMonth() + "/" + e.getPeriod().getYear()) : "");
        }
        @Override public Object getSnapshot(Long id) { return PayrollEntryResponse.from(loadEntry(id)); }

        @Override
        public void applyEdit(Long id, String json) {
            try {
                PayrollEntryRequest req = objectMapper.readValue(json, PayrollEntryRequest.class);
                ApprovalContext.setApplying(true);
                try { service.updateEntry(id, req); } finally { ApprovalContext.clear(); }
            } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
        }

        @Override
        public void applyDelete(Long id) {
            ApprovalContext.setApplying(true);
            try { service.removeEntry(id); } finally { ApprovalContext.clear(); }
        }

        private PayrollEntry loadEntry(Long id) {
            return entryRepo.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payroll entry", id));
        }
    }
}
