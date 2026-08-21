package com.ces.erp.common.exception;

import com.ces.erp.approval.dto.PendingOperationResponse;
import com.ces.erp.approval.exception.PendingApprovalException;
import com.ces.erp.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 202 Accepted ─────────────────────────────────────────────────────────

    @ExceptionHandler(PendingApprovalException.class)
    public ResponseEntity<ApiResponse<PendingOperationResponse>> handlePendingApproval(PendingApprovalException ex) {
        logger.info("Əməliyyat təsdiq gözləyir: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<PendingOperationResponse>builder()
                        .success(true)
                        .message("Əməliyyat təsdiq gözləyir")
                        .data(ex.getOperation())
                        .build());
    }

    // ─── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipart(MultipartException ex) {
        logger.warn("Multipart xətası: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Fayl yüklənərkən xəta baş verdi. Faylın mövcudluğunu və formatını yoxlayın."));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        logger.warn("Biznes qaydası pozuldu: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex) {
        logger.warn("Etibarsız token: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage() != null && !fe.getDefaultMessage().isBlank()
                        ? fe.getDefaultMessage()
                        : fe.getField() + " sahəsi yanlışdır")
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = ex.getBindingResult().getAllErrors().stream()
                    .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Validasiya xətası")
                    .distinct()
                    .collect(Collectors.joining("; "));
        }

        if (message.isBlank()) {
            message = "Daxil edilən məlumatların düzgünlüyünü yoxlayın (Validasiya xətası)";
        }

        logger.warn("Validasiya xətası: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Göndərilən məlumat formatı düzgün deyil. Xanaların tipini (tarix, seçim və s.) yoxlayın.";
        logger.warn("Oxunmaz sorğu gövdəsi: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        logger.warn("Əksik parametr: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Tələb olunan parametr çatışmır: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "'" + ex.getName() + "' parametri üçün düzgün tip tələb olunur: "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "naməlum");
        logger.warn("Tip uyuşmazlığı: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Yanlış arqument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    // ─── 401 Unauthorized ─────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Yanlış giriş məlumatları");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email və ya şifrə yanlışdır"));
    }

    // ─── 403 Forbidden ────────────────────────────────────────────────────────

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
        logger.warn("İcazə rədd edildi: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bu əməliyyat üçün icazəniz yoxdur"));
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedOperation(UnauthorizedOperationException ex) {
        logger.warn("İcazəsiz əməliyyat: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Resurs tapılmadı: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Axtarılan səhifə və ya ünvan tapılmadı: " + ex.getResourcePath()));
    }

    // ─── 409 Conflict ─────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        logger.warn("Duplikat resurs: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─── 422 Unprocessable Entity ─────────────────────────────────────────────

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        logger.warn("Yanlış status keçidi: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─── 413 Payload Too Large ────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logger.warn("Fayl ölçüsü həddən böyükdür: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Fayl ölçüsü həddən böyükdür. Maksimum icazə verilən ölçü: 50MB"));
    }

    // ─── 409 Conflict (DB constraint) ────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String lower = (rootMsg != null ? rootMsg : "").toLowerCase();

        String userFriendlyMsg;
        if (lower.contains("foreign key") || lower.contains("violates foreign key constraint") || lower.contains("referential integrity")) {
            userFriendlyMsg = "Bu qeyd digər məlumatlarla (layihələr, sənədlər, ödənişlər və ya texnikalar) əlaqəli olduğu üçün silinə və ya dəyişdirilə bilməz. Əvvəlcə əlaqəli qeydləri tənzimləyin.";
        } else if (lower.contains("unique constraint") || lower.contains("duplicate key")) {
            userFriendlyMsg = "Daxil edilən unikal məlumat (məsələn: VÖEN, nömrə, kod və ya ad) artıq sistemdə mövcuddur. Təkrar daxil edilə bilməz.";
        } else if (lower.contains("not-null") || lower.contains("null value in column")) {
            userFriendlyMsg = "Tələb olunan məcburi xanalar doldurulmayıb.";
        } else {
            userFriendlyMsg = "Məlumat bazası məhdudiyyəti: Göndərilən məlumatlar bütövlük qaydalarına uyğun gəlmir.";
        }

        logger.warn("Verilənlər bazası məhdudiyyəti pozuldu: {} -> Cavab: {}", ex.getMessage(), userFriendlyMsg);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(userFriendlyMsg));
    }

    // ─── 500 Internal Server Error ────────────────────────────────────────────

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorage(FileStorageException ex) {
        logger.error("Fayl saxlama xətası: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Fayl saxlama xətası: " + ex.getMessage()));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException ex) {
        logger.error("NullPointerException baş verdi: {}", ex.getMessage(), ex);
        String msg = "Daxili server xətası (NullPointerException): Tələb olunan obyekt və ya parametr boşdur (null referans).";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String exceptionType = rootCause.getClass().getSimpleName();
        String rootMsg = rootCause.getMessage();
        if (rootMsg == null || rootMsg.isBlank()) {
            rootMsg = ex.getMessage();
        }

        String userFriendlyMsg;
        if (rootMsg != null && !rootMsg.isBlank()) {
            userFriendlyMsg = "Daxili server xətası (" + exceptionType + "): " + rootMsg;
        } else {
            userFriendlyMsg = "Daxili server xətası (" + exceptionType + "): Gözlənilməz sistem nasazlığı baş verdi.";
        }

        logger.error("Gözlənilməz daxili xəta [{}]: {}", exceptionType, rootMsg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(userFriendlyMsg));
    }
}
