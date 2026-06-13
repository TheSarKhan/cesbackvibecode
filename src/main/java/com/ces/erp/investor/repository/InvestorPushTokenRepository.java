package com.ces.erp.investor.repository;

import com.ces.erp.investor.entity.InvestorPushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorPushTokenRepository extends JpaRepository<InvestorPushToken, Long> {

    List<InvestorPushToken> findAllByInvestorId(Long investorId);

    Optional<InvestorPushToken> findByToken(String token);

    void deleteByToken(String token);
}
