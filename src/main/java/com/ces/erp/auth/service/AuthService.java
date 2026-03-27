package com.ces.erp.auth.service;

import com.ces.erp.auth.dto.LoginRequest;
import com.ces.erp.auth.dto.LoginResponse;
import com.ces.erp.auth.dto.RefreshTokenRequest;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.security.JwtUtil;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditService auditService;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Redis key formatları
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String OTP_PREFIX   = "otp:";
    private static final String VERIFY_PREFIX = "verify:";

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BusinessException("İstifadəçi tapılmadı"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.log("SİSTEM", user.getId(), user.getFullName(), "GİRİŞ ETDİ", user.getEmail() + " sistemə daxil oldu");
        return buildLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshTokenRequest request) {
        String userIdStr = redisTemplate.opsForValue().get(REFRESH_PREFIX + request.getRefreshToken());
        if (userIdStr == null) {
            throw new BusinessException("Refresh token etibarsızdır və ya vaxtı keçib");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("İstifadəçi tapılmadı"));

        // Köhnə tokeni sil, yeni token yarat
        redisTemplate.delete(REFRESH_PREFIX + request.getRefreshToken());
        return buildLoginResponse(user);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException("Bu email sistemdə qeydiyyatda deyil"));

        // 6 rəqəmli OTP kod yarat
        String otp = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, 600, TimeUnit.SECONDS); // 10 dəqiqə

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("CES ERP - Şifrə Yeniləmə OTP Kodu");
        msg.setText(
            "Salam " + user.getFullName() + ",\n\n" +
            "Şifrənizi yeniləmək üçün aşağıdakı 6 rəqəmli kodu istifadə edin:\n\n" +
            otp + "\n\n" +
            "Bu kod 10 dəqiqə ərzində etibarlıdır.\n\n" +
            "Əgər bu sorğunu siz etməmisinizsə, bu emaili nəzərə almayın.\n\n" +
            "CES ERP Sistemi"
        );

        try {
            mailSender.send(msg);
            logger.info("✓ OTP maili göndərildi: {}", email);
        } catch (Exception e) {
            logger.error("✗ OTP maili göndərərkən XƏTA: {} | Səbəb: {}", email, e.getMessage(), e);
            throw new BusinessException("Email göndərməkdə xəta baş verdi. Zəhmət olmasa yenidən cəhd edin.");
        }
    }

    public String verifyOtp(String email, String otp) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new BusinessException("OTP kod etibarsızdır və ya vaxtı keçib");
        }

        // Verification token yarat
        String verificationToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(VERIFY_PREFIX + verificationToken, email, 1800, TimeUnit.SECONDS); // 30 dəqiqə
        redisTemplate.delete(OTP_PREFIX + email); // OTP-ni sil

        logger.info("✓ OTP doğrulandı: {}", email);
        return verificationToken;
    }

    @Transactional
    public void resetPassword(String verificationToken, String newPassword) {
        String email = redisTemplate.opsForValue().get(VERIFY_PREFIX + verificationToken);
        if (email == null) {
            throw new BusinessException("Doğrulama token-i etibarsızdır və ya vaxtı keçib");
        }

        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException("İstifadəçi tapılmadı"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(VERIFY_PREFIX + verificationToken);

        auditService.log("SİSTEM", user.getId(), user.getFullName(), "YENİLƏNDİ", "Şifrə yeniləndi");
        logger.info("✓ Şifrə yeniləndi: {}", email);
    }

    public void sendTestEmail(String email) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("CES ERP - Test Email");
        msg.setText("Salam,\n\nBu bir test emailidir.\n\nCES ERP");

        try {
            mailSender.send(msg);
            logger.info("✓ Test maili göndərildi: {}", email);
        } catch (Exception e) {
            logger.error("✗ Test maili göndərərkən XƏTA: {} | Səbəb: {}", email, e.getMessage(), e);
            e.printStackTrace();
            throw new BusinessException("Test maili göndərərkən xəta: " + e.getMessage());
        }
    }

    public void logout(String refreshToken) {
        String userIdStr = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                userRepository.findByIdAndDeletedFalse(userId).ifPresent(user ->
                    auditService.log("SİSTEM", user.getId(), user.getFullName(), "ÇIXIŞ ETDİ", user.getEmail() + " sistemdən çıxdı")
                );
            } catch (NumberFormatException ignored) {}
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

        // Modül icazələrini topla
        List<LoginResponse.ModulePermission> permissions = List.of();
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            permissions = user.getRole().getPermissions().stream()
                    .map(p -> LoginResponse.ModulePermission.builder()
                            .moduleCode(p.getModule().getCode())
                            .moduleNameAz(p.getModule().getNameAz())
                            .canGet(p.isCanGet())
                            .canPost(p.isCanPost())
                            .canPut(p.isCanPut())
                            .canDelete(p.isCanDelete())
                            .canSendToCoordinator(p.isCanSendToCoordinator())
                            .canSubmitOffer(p.isCanSubmitOffer())
                            .build())
                    .toList();
        }

        // Approval şöbələri
        List<String> approvalDepts = user.getApprovalDepartments().stream()
                .map(ad -> ad.getDepartment().getName())
                .toList();

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .hasApproval(user.isHasApproval())
                .approvalDepartments(approvalDepts)
                .permissions(permissions)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}
