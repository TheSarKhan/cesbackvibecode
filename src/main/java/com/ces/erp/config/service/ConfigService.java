package com.ces.erp.config.service;

import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.config.dto.ConfigItemRequest;
import com.ces.erp.config.dto.ConfigItemResponse;
import com.ces.erp.config.entity.ConfigItem;
import com.ces.erp.config.repository.ConfigItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigItemRepository repository;
    private final AuditService auditService;

    public Map<String, List<ConfigItemResponse>> getAllGrouped() {
        return repository.findAllByDeletedFalseOrderByCategoryAscSortOrderAsc().stream()
                .map(ConfigItemResponse::from)
                .collect(Collectors.groupingBy(ConfigItemResponse::getCategory, LinkedHashMap::new, Collectors.toList()));
    }

    public PagedResponse<ConfigItemResponse> getAllPaged(int page, int size, String search, String category) {
        String q = (search != null && !search.isBlank()) ? search : null;
        String cat = (category != null && !category.isBlank()) ? category : null;
        var pageable = PageRequest.of(page, size);
        return PagedResponse.from(repository.findAllFiltered(q, cat, pageable), ConfigItemResponse::from);
    }

    public List<ConfigItemResponse> getByCategory(String category) {
        return repository.findAllByCategoryAndDeletedFalseOrderBySortOrderAsc(category).stream()
                .map(ConfigItemResponse::from)
                .toList();
    }

    public List<ConfigItemResponse> getActiveByCategory(String category) {
        return repository.findAllByCategoryAndActiveTrueAndDeletedFalseOrderBySortOrderAsc(category).stream()
                .map(ConfigItemResponse::from)
                .toList();
    }

    public List<String> getCategories() {
        return repository.findDistinctCategories();
    }

    public ConfigItemResponse getById(Long id) {
        return ConfigItemResponse.from(findOrThrow(id));
    }

    @Transactional
    public ConfigItemResponse create(ConfigItemRequest req) {
        if (repository.existsByCategoryAndKeyAndDeletedFalse(req.getCategory(), req.getKey())) {
            throw new BusinessException("Bu kateqoriyada belə bir açar artıq mövcuddur: " + req.getKey());
        }

        ConfigItem entity = ConfigItem.builder()
                .category(req.getCategory().toUpperCase().trim())
                .key(req.getKey().trim())
                .value(req.getValue())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder())
                .active(req.isActive())
                .build();

        ConfigItem saved = repository.save(entity);
        auditService.log("KONFİQURASİYA", saved.getId(), saved.getCategory() + ":" + saved.getKey(),
                "YARADILDI", "Konfiqurasiya elementi yaradıldı");
        return ConfigItemResponse.from(saved);
    }

    @Transactional
    public ConfigItemResponse update(Long id, ConfigItemRequest req) {
        ConfigItem entity = findOrThrow(id);

        // Check unique constraint if key/category changed
        if (!entity.getCategory().equals(req.getCategory().toUpperCase().trim())
                || !entity.getKey().equals(req.getKey().trim())) {
            if (repository.existsByCategoryAndKeyAndDeletedFalse(req.getCategory().toUpperCase().trim(), req.getKey().trim())) {
                throw new BusinessException("Bu kateqoriyada belə bir açar artıq mövcuddur: " + req.getKey());
            }
        }

        entity.setCategory(req.getCategory().toUpperCase().trim());
        entity.setKey(req.getKey().trim());
        entity.setValue(req.getValue());
        entity.setDescription(req.getDescription());
        entity.setSortOrder(req.getSortOrder());
        entity.setActive(req.isActive());

        ConfigItem saved = repository.save(entity);
        auditService.log("KONFİQURASİYA", saved.getId(), saved.getCategory() + ":" + saved.getKey(),
                "YENİLƏNDİ", "Konfiqurasiya elementi yeniləndi");
        return ConfigItemResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        ConfigItem entity = findOrThrow(id);
        auditService.log("KONFİQURASİYA", entity.getId(), entity.getCategory() + ":" + entity.getKey(),
                "SİLİNDİ", "Konfiqurasiya elementi silindi");
        entity.softDelete();
        repository.save(entity);
    }

    @Transactional
    public void reorder(String category, List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            ConfigItem item = findOrThrow(orderedIds.get(i));
            item.setSortOrder(i);
            repository.save(item);
        }
    }

    private ConfigItem findOrThrow(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Konfiqurasiya elementi", id));
    }
}
