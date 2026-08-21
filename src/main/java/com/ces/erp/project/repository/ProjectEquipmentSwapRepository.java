package com.ces.erp.project.repository;

import com.ces.erp.project.entity.ProjectEquipmentSwap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectEquipmentSwapRepository extends JpaRepository<ProjectEquipmentSwap, Long> {
    List<ProjectEquipmentSwap> findByProjectIdAndDeletedFalseOrderBySwapDateDesc(Long projectId);
}
