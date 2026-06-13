package com.ces.erp.investor.repository;

import com.ces.erp.investor.entity.InvestorNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestorNotificationRepository extends JpaRepository<InvestorNotification, Long> {

    List<InvestorNotification> findTop50ByInvestorIdOrderByCreatedAtDesc(Long investorId);

    long countByInvestorIdAndReadFalse(Long investorId);

    Optional<InvestorNotification> findByIdAndInvestorId(Long id, Long investorId);

    @Modifying
    @Query("UPDATE InvestorNotification n SET n.read = true WHERE n.investor.id = :investorId AND n.read = false")
    void markAllRead(@Param("investorId") Long investorId);
}
