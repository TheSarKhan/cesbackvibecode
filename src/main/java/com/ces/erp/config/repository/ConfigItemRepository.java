package com.ces.erp.config.repository;

import com.ces.erp.config.entity.ConfigItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT c FROM ConfigItem c WHERE c.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(c.key) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.value, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:category AS string) IS NULL OR c.category = :category)" +
            " ORDER BY c.category ASC, c.sortOrder ASC")
    Page<ConfigItem> findAllFiltered(@Param("search") String search,
                                     @Param("category") String category,
                                     Pageable pageable);

    List<ConfigItem> findAllByDeletedTrue();
}
