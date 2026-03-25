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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

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
    private static final String RESET_PREFIX   = "pwreset:";

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
        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(RESET_PREFIX + token, String.valueOf(user.getId()), 3600, TimeUnit.SECONDS);

            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("CES ERP - Şifrə yeniləmə");
            msg.setText(
                "Salam " + user.getFullName() + ",\n\n" +
                "Şifrənizi yeniləmək üçün aşağıdakı linkə klikləyin:\n\n" +
                resetUrl + "\n\n" +
                "Bu link 1 saat ərzində etibarlıdır.\n\n" +
                "Əgər bu sorğunu siz etməmisinizsə, bu emaili nəzərə almayın.\n\n" +
                "CES ERP Sistemi"
            );
            try { mailSender.send(msg); } catch (Exception ignored) {}
        });
        // Email mövcud olub-olmadığını açıqlamamaq üçün həmişə uğurlu cavab
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String userIdStr = redisTemplate.opsForValue().get(RESET_PREFIX + token);
        if (userIdStr == null) {
            throw new BusinessException("Link etibarsızdır və ya vaxtı keçib");
        }
        User user = userRepository.findByIdAndDeletedFalse(Long.parseLong(userIdStr))
                .orElseThrow(() -> new BusinessException("İstifadəçi tapılmadı"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(RESET_PREFIX + token);

        auditService.log("SİSTEM", user.getId(), user.getFullName(), "YENİLƏNDİ", "Şifrə yeniləndi");
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
