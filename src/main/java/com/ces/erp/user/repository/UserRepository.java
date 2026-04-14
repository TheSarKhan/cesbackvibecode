package com.ces.erp.user.repository;

import com.ces.erp.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    List<User> findAllByDeletedFalse();

    @Query("SELECT u FROM User u WHERE u.deleted = false" +
            " AND (CAST(:search AS string) IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            " OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))" +
            " AND (:departmentId IS NULL OR u.department.id = :departmentId)")
    Page<User> findAllFiltered(@Param("search") String search,
                               @Param("departmentId") Long departmentId,
                               Pageable pageable);

    List<User> findAllByDepartmentIdAndDeletedFalse(Long departmentId);

    List<User> findAllByRoleIdAndDeletedFalse(Long roleId);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.module WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailWithPermissions(@Param("email") String email);

    List<User> findAllByDeletedTrue();

    long countByDeletedFalseAndActiveTrue();

    long countByDeletedTrue();
}
