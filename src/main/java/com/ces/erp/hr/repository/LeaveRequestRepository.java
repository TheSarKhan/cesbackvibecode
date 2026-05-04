package com.ces.erp.hr.repository;

import com.ces.erp.enums.LeaveStatus;
import com.ces.erp.hr.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByIdAndDeletedFalse(Long id);

    List<LeaveRequest> findAllByEmployeeIdAndDeletedFalseOrderByStartDateDesc(Long employeeId);

    long countByStatusAndDeletedFalse(LeaveStatus status);

    @Query("""
        SELECT l FROM LeaveRequest l
        WHERE l.deleted = false
          AND (:employeeId IS NULL OR l.employee.id = :employeeId)
          AND (:status IS NULL OR l.status = :status)
        ORDER BY l.startDate DESC
        """)
    Page<LeaveRequest> searchPaged(@Param("employeeId") Long employeeId,
                                   @Param("status") LeaveStatus status,
                                   Pageable pageable);

    // İşçinin müəyyən bir tarixdə təsdiqlənmiş məzuniyyəti varmı?
    @Query("""
        SELECT l FROM LeaveRequest l
        WHERE l.deleted = false AND l.status = com.ces.erp.enums.LeaveStatus.APPROVED
          AND l.employee.id = :employeeId
          AND l.startDate <= :date AND l.endDate >= :date
        """)
    Optional<LeaveRequest> findApprovedLeaveOnDate(@Param("employeeId") Long employeeId,
                                                   @Param("date") LocalDate date);

    // İşçinin il ərzində istifadə etdiyi məzuniyyət günləri (təsdiqlənmiş, müəyyən tip)
    @Query("""
        SELECT COALESCE(SUM(l.days), 0) FROM LeaveRequest l
        WHERE l.deleted = false AND l.status = com.ces.erp.enums.LeaveStatus.APPROVED
          AND l.employee.id = :employeeId
          AND YEAR(l.startDate) = :year
          AND l.type = com.ces.erp.enums.LeaveType.ANNUAL
        """)
    Integer sumApprovedAnnualLeaveDaysInYear(@Param("employeeId") Long employeeId,
                                             @Param("year") Integer year);
}
