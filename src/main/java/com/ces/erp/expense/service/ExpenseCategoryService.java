package com.ces.erp.expense.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.DuplicateResourceException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.expense.dto.ExpenseCategoryRequest;
import com.ces.erp.expense.dto.ExpenseCategoryResponse;
import com.ces.erp.expense.entity.ExpenseCategory;
import com.ces.erp.expense.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final AuditService auditService;

    public List<ExpenseCategoryResponse> getAll() {
        return categoryRepository.findAllByDeletedFalseOrderByNameAsc().stream()
                .map(ExpenseCategoryResponse::from)
                .toList();
    }

    public List<ExpenseCategoryResponse> getAllActive() {
        return categoryRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc().stream()
                .map(ExpenseCategoryResponse::from)
                .toList();
    }

    public ExpenseCategoryResponse getById(Long id) {
        return ExpenseCategoryResponse.from(findOrThrow(id));
    }

    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest request) {
        String code = request.getCode().toUpperCase().trim();
        if (categoryRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Bu kod artıq mövcuddur: " + code);
        }
        ExpenseCategory saved = categoryRepository.save(
                ExpenseCategory.builder()
                        .code(code)
                        .name(request.getName().trim())
                        .description(request.getDescription())
                        .active(request.isActive())
                        .build()
        );
        auditService.log("XƏRC_KATEQORİYASI", saved.getId(), saved.getName(), "YARADILDI", "Yeni xərc kateqoriyası");
        return ExpenseCategoryResponse.from(saved);
    }

    @Transactional
    public ExpenseCategoryResponse update(Long id, ExpenseCategoryRequest request) {
        ExpenseCategory cat = findOrThrow(id);
        String code = request.getCode().toUpperCase().trim();
        if (categoryRepository.existsByCodeAndIdNotAndDeletedFalse(code, id)) {
            throw new DuplicateResourceException("Bu kod artıq mövcuddur: " + code);
        }
        cat.setCode(code);
        cat.setName(request.getName().trim());
        cat.setDescription(request.getDescription());
        cat.setActive(request.isActive());
        ExpenseCategory saved = categoryRepository.save(cat);
        auditService.log("XƏRC_KATEQORİYASI", saved.getId(), saved.getName(), "YENİLƏNDİ", "Kateqoriya yeniləndi");
        return ExpenseCategoryResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        ExpenseCategory cat = findOrThrow(id);
        auditService.log("XƏRC_KATEQORİYASI", cat.getId(), cat.getName(), "SİLİNDİ", "Kateqoriya silindi");
        cat.softDelete();
        categoryRepository.save(cat);
    }

    private ExpenseCategory findOrThrow(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Xərc kateqoriyası", id));
    }
}
