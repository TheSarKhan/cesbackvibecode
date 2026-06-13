package com.ces.erp.investor.service;

import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.RefreshTokenRequest;
import com.ces.erp.common.exception.InvalidTokenException;
import com.ces.erp.common.security.JwtUtil;
import com.ces.erp.investor.dto.InvestorLoginResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Investor portal kimlik doğrulaması — internal {@code AuthService}-dən izolyasiyalı.
 * Refresh token-lər Redis-də ayrı namespace-də saxlanır: {@code investor_refresh:<uuid>} → investorId.
 */
@Service
@RequiredArgsConstructor
public class InvestorAuthService {

    private final InvestorRepository investorRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private static final String REFRESH_PREFIX = "investor_refresh:";

    @Transactional
    public InvestorLoginResponse login(LoginRequest request) {
        Investor investor = investorRepository
                .findByAccountEmailIgnoreCaseAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email və ya şifrə yanlışdır"));

        // Portal bağlıdırsa və ya şifrə təyin edilməyibsə / yanlışdırsa → 401
        if (!investor.isPortalEnabled()
                || investor.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), investor.getPasswordHash())) {
            throw new BadCredentialsException("Email və ya şifrə yanlışdır");
        }

        investor.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        investorRepository.save(investor);

        return buildLoginResponse(investor);
    }

    @Transactional(readOnly = true)
    public InvestorLoginResponse refresh(RefreshTokenRequest request) {
        String key = REFRESH_PREFIX + request.getRefreshToken();
        String investorIdStr = redisTemplate.opsForValue().get(key);
        if (investorIdStr == null) {
            throw new InvalidTokenException("Refresh token etibarsızdır və ya vaxtı keçib");
        }

        Long investorId = Long.parseLong(investorIdStr);
        Investor investor = investorRepository.findByIdAndDeletedFalse(investorId)
                .orElseThrow(() -> new InvalidTokenException("İnvestor tapılmadı"));

        if (!investor.isPortalEnabled()) {
            throw new InvalidTokenException("Portal girişi bağlıdır");
        }

        // Köhnə refresh-i sil, yenisini ver (rotation)
        redisTemplate.delete(key);
        return buildLoginResponse(investor);
    }

    public void logout(String refreshToken) {
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
    }

    private InvestorLoginResponse buildLoginResponse(Investor investor) {
        String accessToken = jwtUtil.generateInvestorAccessToken(investor.getId(), investor.getAccountEmail());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + refreshToken,
                String.valueOf(investor.getId()),
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

        return InvestorLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .investor(InvestorLoginResponse.InvestorInfo.builder()
                        .id(investor.getId())
                        .companyName(investor.getCompanyName())
                        .accountEmail(investor.getAccountEmail())
                        .contactPerson(investor.getContactPerson())
                        .contactPhone(investor.getContactPhone())
                        .build())
                .build();
    }
}
