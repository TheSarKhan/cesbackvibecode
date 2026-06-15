package com.ces.erp.hr.service;

import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.hr.dto.DeductionTypeDto;
import com.ces.erp.hr.entity.DeductionConfigVersion;
import com.ces.erp.hr.entity.DeductionType;
import com.ces.erp.hr.repository.DeductionConfigVersionRepository;
import com.ces.erp.hr.repository.DeductionTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DeductionConfigService iki entity tipi idarə etdiyi üçün təsdiq sistemi
 * iki ayrı ApprovalHandler bean-i ilə dəstəklənir.
 */
public class DeductionApprovalHandlers {

    @Component
    @RequiredArgsConstructor
    public static class TypeHandler implements ApprovalHandler {
        private final DeductionConfigService service;
        private final DeductionTypeRepository typeRepo;
        private final ObjectMapper objectMapper;

        @Override public String getEntityType() { return "DEDUCTION_TYPE"; }
        @Override public String getModuleCode()  { return "HR"; }
        @Override public String getLabel(Long id) { return loadType(id).getCode(); }
        @Override public Object getSnapshot(Long id) { return DeductionTypeDto.from(loadType(id)); }

        @Override
        public void applyEdit(Long id, String json) {
            try {
                DeductionTypeDto dto = objectMapper.readValue(json, DeductionTypeDto.class);
                ApprovalContext.setApplying(true);
                try { service.updateType(id, dto); } finally { ApprovalContext.clear(); }
            } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
        }

        @Override
        public void applyDelete(Long id) {
            ApprovalContext.setApplying(true);
            try { service.deleteType(id); } finally { ApprovalContext.clear(); }
        }

        private DeductionType loadType(Long id) {
            return typeRepo.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Tutulma növü", id));
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class VersionHandler implements ApprovalHandler {
        private final DeductionConfigService service;
        private final DeductionConfigVersionRepository versionRepo;

        @Override public String getEntityType() { return "DEDUCTION_VERSION"; }
        @Override public String getModuleCode()  { return "HR"; }
        @Override public String getLabel(Long id) { return "v" + loadVersion(id).getVersionNo(); }
        @Override public Object getSnapshot(Long id) { return service.getVersion(id); }

        @Override
        public void applyEdit(Long id, String json) {
            throw new BusinessException("Tutulma versiyası üçün edit dəstəklənmir");
        }

        @Override
        public void applyDelete(Long id) {
            ApprovalContext.setApplying(true);
            try { service.deleteVersion(id); } finally { ApprovalContext.clear(); }
        }

        private DeductionConfigVersion loadVersion(Long id) {
            return versionRepo.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Versiya", id));
        }
    }
}
