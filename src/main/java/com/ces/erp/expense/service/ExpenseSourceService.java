package com.ces.erp.expense.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.expense.dto.ExpenseSourceRequest;
import com.ces.erp.expense.dto.ExpenseSourceResponse;
import com.ces.erp.expense.entity.ExpenseCategory;
import com.ces.erp.expense.entity.ExpenseSource;
import com.ces.erp.expense.repository.ExpenseCategoryRepository;
import com.ces.erp.expense.repository.ExpenseSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseSourceService {

    private final ExpenseSourceRepository sourceRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final AuditService auditService;

    public List<ExpenseSourceResponse> getAll() {
        return sourceRepository.findAllActive().stream()
                .map(ExpenseSourceResponse::from)
                .toList();
    }

    public List<ExpenseSourceResponse> getAllActive() {
        return sourceRepository.findAllActiveAndEnabled().stream()
                .map(ExpenseSourceResponse::from)
                .toList();
    }

    public List<ExpenseSourceResponse> getByCategory(Long categoryId) {
        return sourceRepository.findAllByCategoryId(categoryId).stream()
                .map(ExpenseSourceResponse::from)
                .toList();
    }

    public List<ExpenseSourceResponse> getActiveByCategoryId(Long categoryId) {
        return sourceRepository.findAllByCategoryIdAndActiveTrue(categoryId).stream()
                .map(ExpenseSourceResponse::from)
                .toList();
    }

    public ExpenseSourceResponse getById(Long id) {
        return ExpenseSourceResponse.from(findOrThrow(id));
    }

    @Transactional
    public ExpenseSourceResponse create(ExpenseSourceRequest request) {
        ExpenseCategory category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Xərc kateqoriyası", request.getCategoryId()));
        String code = request.getCode().toUpperCase().trim();
        if (sourceRepository.existsByCodeAndCategoryIdAndDeletedFalse(code, request.getCategoryId())) {
            throw new BusinessException("Bu kateqoriyada bu kod artıq mövcuddur: " + code);
        }
        ExpenseSource saved = sourceRepository.save(
                ExpenseSource.builder()
                        .code(code)
                        .name(request.getName().trim())
                        .category(category)
                        .active(request.isActive())
                        .build()
        );
        auditService.log("XƏRC_MƏNBƏYİ", saved.getId(), saved.getName(), "YARADILDI", "Yeni xərc mənbəyi");
        return ExpenseSourceResponse.from(saved);
    }

    @Transactional
    public ExpenseSourceResponse update(Long id, ExpenseSourceRequest request) {
        ExpenseSource src = findOrThrow(id);
        ExpenseCategory category = categoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Xərc kateqoriyası", request.getCategoryId()));
        String code = request.getCode().toUpperCase().trim();
        if (sourceRepository.existsByCodeAndCategoryIdAndIdNotAndDeletedFalse(code, request.getCategoryId(), id)) {
            throw new BusinessException("Bu kateqoriyada bu kod artıq mövcuddur: " + code);
        }
        src.setCode(code);
        src.setName(request.getName().trim());
        src.setCategory(category);
        src.setActive(request.isActive());
        ExpenseSource saved = sourceRepository.save(src);
        auditService.log("XƏRC_MƏNBƏYİ", saved.getId(), saved.getName(), "YENİLƏNDİ", "Mənbə yeniləndi");
        return ExpenseSourceResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        ExpenseSource src = findOrThrow(id);
        auditService.log("XƏRC_MƏNBƏYİ", src.getId(), src.getName(), "SİLİNDİ", "Mənbə silindi");
        src.softDelete();
        sourceRepository.save(src);
    }

    private ExpenseSource findOrThrow(Long id) {
        return sourceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Xərc mənbəyi", id));
    }
}
