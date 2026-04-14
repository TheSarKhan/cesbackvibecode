package com.ces.erp.technicalservice.repository;

import com.ces.erp.technicalservice.entity.ServiceChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceChecklistItemRepository extends JpaRepository<ServiceChecklistItem, Long> {
}
