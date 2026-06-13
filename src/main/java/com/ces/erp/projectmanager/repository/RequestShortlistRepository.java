package com.ces.erp.projectmanager.repository;

import com.ces.erp.projectmanager.entity.RequestShortlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestShortlistRepository extends JpaRepository<RequestShortlist, Long> {

    Optional<RequestShortlist> findByRequestIdAndDeletedFalse(Long requestId);

    boolean existsByRequestIdAndDeletedFalse(Long requestId);
}
