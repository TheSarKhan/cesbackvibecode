package com.ces.erp.request.repository;

import com.ces.erp.request.entity.RequestStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStatusLogRepository extends JpaRepository<RequestStatusLog, Long> {

    List<RequestStatusLog> findAllByRequestIdOrderByChangedAtDesc(Long requestId);
}
