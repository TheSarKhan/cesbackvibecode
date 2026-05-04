package com.ces.erp.hr.repository;

import com.ces.erp.hr.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByIdAndDeletedFalse(Long id);

    Optional<AttendanceRecord> findByEmployeeIdAndDateAndDeletedFalse(Long employeeId, LocalDate date);

    List<AttendanceRecord> findAllByEmployeeIdAndDateBetweenAndDeletedFalseOrderByDateAsc(
            Long employeeId, LocalDate start, LocalDate end);

    List<AttendanceRecord> findAllByDateBetweenAndDeletedFalseOrderByDateAsc(LocalDate start, LocalDate end);
}
