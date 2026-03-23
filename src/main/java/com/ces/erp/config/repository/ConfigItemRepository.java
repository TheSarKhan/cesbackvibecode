package com.ces.erp.config.repository;

import com.ces.erp.config.entity.ConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConfigItemRepository extends JpaRepository<ConfigItem, Long> {

    List<ConfigItem> findAllByDeletedFalseOrderByCategoryAscSortOrderAsc();

    List<ConfigItem> findAllByCategoryAndDeletedFalseOrderBySortOrderAsc(String category);

    List<ConfigItem> findAllByCategoryAndActiveTrueAndDeletedFalseOrderBySortOrderAsc(String category);

    Optional<ConfigItem> findByIdAndDeletedFalse(Long id);

    boolean existsByCategoryAndKeyAndDeletedFalse(String category, String key);

    @Query("SELECT DISTINCT c.category FROM ConfigItem c WHERE c.deleted = false ORDER BY c.category")
    List<String> findDistinctCategories();

    List<ConfigItem> findAllByDeletedTrue();
}
