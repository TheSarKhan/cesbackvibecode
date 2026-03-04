package com.ces.erp.systemmodule.repository;

import com.ces.erp.systemmodule.entity.SystemModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemModuleRepository extends JpaRepository<SystemModule, Long> {

    List<SystemModule> findAllByOrderByOrderIndexAsc();

    Optional<SystemModule> findByCode(String code);

    boolean existsByCode(String code);
}
