package com.ces.erp.projectmanager.repository;

import com.ces.erp.projectmanager.entity.ShortlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortlistItemRepository extends JpaRepository<ShortlistItem, Long> {

    List<ShortlistItem> findAllByShortlistIdAndDeletedFalse(Long shortlistId);

    List<ShortlistItem> findAllByShortlist_Request_IdAndDeletedFalseOrderByRankAscIdAsc(Long requestId);
}
