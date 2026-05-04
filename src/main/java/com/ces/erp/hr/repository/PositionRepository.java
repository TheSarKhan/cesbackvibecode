package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findAllByDeletedFalseOrderByNameAsc();
    Optional<Position> findByIdAndDeletedFalse(Long id);
    boolean existsByNameAndDeletedFalse(String name);
    List<Position> findAllByDepartmentIdAndDeletedFalse(Long departmentId);
}
