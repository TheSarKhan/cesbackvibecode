package com.ces.erp.investor.service;

import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.RefreshTokenRequest;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.InvalidTokenException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.security.JwtUtil;
import com.ces.erp.investor.dto.InvestorLoginResponse;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Investor portal kimlik doğrulaması — internal {@code AuthService}-dən izolyasiyalı.
 * Refresh token-lər Redis-də ayrı namespace-də saxlanır: {@code investor_refresh:<uuid>} → investorId.
 */
@Service
@RequiredArgsConstructor
public class InvestorAuthService {

    private static final Logger logger = LoggerFactory.getLogger(InvestorAuthService.class);

    private final InvestorRepository investorRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private static final String REFRESH_PREFIX = "investor_refresh:";
    private static final String OTP_PREFIX     = "investor_otp:";
    private static final String VERIFY_PREFIX  = "investor_verify:";

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

    // ───── Şifrəni unutdum (OTP → token → reset) ─────────────────────────────

    /** Investor email-inə 6 rəqəmli OTP göndərir (Redis-də 10 dəq saxlanır). */
    public void forgotPassword(String email) {
        String normalized = email.trim().toLowerCase();
        Investor investor = investorRepository
                .findByAccountEmailIgnoreCaseAndDeletedFalse(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Bu email portal hesabı kimi qeydiyyatda deyil"));

        if (!investor.isPortalEnabled()) {
            throw new BusinessException("Portal girişi bağlıdır");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        redisTemplate.opsForValue().set(OTP_PREFIX + normalized, otp, 600, TimeUnit.SECONDS); // 10 dəqiqə

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(investor.getAccountEmail());
        msg.setSubject("Invorent İnvestor Portalı - Şifrə Yeniləmə Kodu");
        msg.setText(
            "Salam " + (investor.getContactPerson() != null ? investor.getContactPerson() : investor.getCompanyName()) + ",\n\n" +
            "Şifrənizi yeniləmək üçün aşağıdakı 6 rəqəmli kodu istifadə edin:\n\n" +
            otp + "\n\n" +
            "Bu kod 10 dəqiqə ərzində etibarlıdır.\n\n" +
            "Əgər bu sorğunu siz etməmisinizsə, bu emaili nəzərə almayın.\n\n" +
            "Invorent İnvestor Portalı"
        );

        try {
            mailSender.send(msg);
            logger.info("✓ İnvestor OTP maili göndərildi: {}", normalized);
        } catch (Exception e) {
            logger.error("✗ İnvestor OTP maili göndərilmədi: {} | {}", normalized, e.getMessage(), e);
            throw new BusinessException("Email göndərməkdə xəta baş verdi. Zəhmət olmasa yenidən cəhd edin.");
        }
    }

    /** OTP-ni yoxlayır, doğrudursa 30 dəq etibarlı verification token qaytarır. */
    public String verifyOtp(String email, String otp) {
        String normalized = email.trim().toLowerCase();
        String stored = redisTemplate.opsForValue().get(OTP_PREFIX + normalized);
        if (stored == null || !stored.equals(otp)) {
            throw new InvalidTokenException("OTP kod etibarsızdır və ya vaxtı keçib");
        }

        String verificationToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(VERIFY_PREFIX + verificationToken, normalized, 1800, TimeUnit.SECONDS); // 30 dəqiqə
        redisTemplate.delete(OTP_PREFIX + normalized);
        return verificationToken;
    }

    /** Verification token ilə yeni şifrəni təyin edir. */
    @Transactional
    public void resetPassword(String verificationToken, String newPassword) {
        String email = redisTemplate.opsForValue().get(VERIFY_PREFIX + verificationToken);
        if (email == null) {
            throw new InvalidTokenException("Doğrulama token-i etibarsızdır və ya vaxtı keçib");
        }

        Investor investor = investorRepository
                .findByAccountEmailIgnoreCaseAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("İnvestor tapılmadı"));

        investor.setPasswordHash(passwordEncoder.encode(newPassword));
        investorRepository.save(investor);
        redisTemplate.delete(VERIFY_PREFIX + verificationToken);
        logger.info("✓ İnvestor şifrəsi yeniləndi: {}", email);
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
