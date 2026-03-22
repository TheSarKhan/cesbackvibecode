package com.ces.erp.request.repository;

import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.TechRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TechRequestRepository extends JpaRepository<TechRequest, Long> {

    @Query("SELECT r FROM TechRequest r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.selectedEquipment LEFT JOIN FETCH r.createdBy WHERE r.deleted = false ORDER BY r.createdAt DESC")
    List<TechRequest> findAllByDeletedFalse();

    @Query("SELECT r FROM TechRequest r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.selectedEquipment LEFT JOIN FETCH r.createdBy WHERE r.id = :id AND r.deleted = false")
    Optional<TechRequest> findByIdAndDeletedFalse(Long id);

    List<TechRequest> findAllByStatusAndDeletedFalse(RequestStatus status);

    @Query("SELECT r FROM TechRequest r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.selectedEquipment e LEFT JOIN FETCH e.ownerContractor LEFT JOIN FETCH r.createdBy WHERE r.status IN :statuses AND r.deleted = false ORDER BY r.createdAt DESC")
    List<TechRequest> findAllByStatusInAndDeletedFalse(@Param("statuses") List<RequestStatus> statuses);

    List<TechRequest> findAllByDeletedTrue();

    @Query("SELECT COUNT(r) FROM TechRequest r WHERE r.status IN :statuses AND r.deleted = false")
    long countByStatusInAndDeletedFalse(@Param("statuses") List<RequestStatus> statuses);

    @Query("SELECT COUNT(r) FROM TechRequest r WHERE r.deleted = true")
    long countByDeletedTrue();

    @EntityGraph(attributePaths = {"customer", "selectedEquipment", "createdBy"})
    @Query("SELECT r FROM TechRequest r WHERE r.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(r.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.requestCode, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.projectName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(COALESCE(r.region, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (CAST(:status AS string) IS NULL OR r.status = :status)" +
            " AND (CAST(:region AS string) IS NULL OR r.region = :region)" +
            " AND (CAST(:projectType AS string) IS NULL OR r.projectType = :projectType)")
    Page<TechRequest> findAllFiltered(
            @Param("search") String search,
            @Param("status") RequestStatus status,
            @Param("region") String region,
            @Param("projectType") String projectType,
            Pageable pageable);
}
