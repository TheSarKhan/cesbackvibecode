package com.ces.erp.permission;

import com.ces.erp.permission.entity.Permission;
import com.ces.erp.permission.repository.PermissionRepository;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tətbiq açılanda bütün controller-lərdəki {@code @PreAuthorize("hasAuthority('MODULE:ACTION')")}
 * string-lərini skan edib dinamik icazə kataloquna ({@link Permission}) upsert edir.
 * Beləcə yeni endpoint etiketlənəndə icazə proqramçı əlavə iş görmədən kataloqda görünür.
 * <p>
 * {@code @Order(20)} — seeder-lərdən sonra işləyir ki, modullar mövcud olsun və etiketlər
 * düzgün modul adı ilə qurulsun. (Seeder grant-ları icazələri find-or-create ilə özü yaradır,
 * ona görə scanner-dən asılı deyil.)
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class PermissionScanner implements CommandLineRunner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionRepository permissionRepository;
    private final SystemModuleRepository moduleRepository;

    // hasAuthority('X:Y') / hasAnyAuthority('X:Y', ...) içindəki MODULE:ACTION kodları
    private static final Pattern CODE = Pattern.compile("'([A-Z0-9_]+:[A-Z0-9_]+)'");

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> realCodes = collectAuthorityCodes();

        // 1) Yeni real icazələri əlavə et (mövcud label-a toxunmadan)
        int created = 0;
        for (String code : realCodes) {
            if (permissionRepository.findByCode(code).isPresent()) continue;
            int sep = code.indexOf(':');
            String moduleCode = code.substring(0, sep);
            String action = code.substring(sep + 1);
            String moduleNameAz = moduleRepository.findByCode(moduleCode)
                    .map(m -> m.getNameAz()).orElse(moduleCode);
            permissionRepository.save(Permission.builder()
                    .code(code)
                    .moduleCode(moduleCode)
                    .action(action)
                    .labelAz(PermissionLabels.fullLabel(moduleNameAz, action))
                    .autoDiscovered(true)
                    .build());
            created++;
        }

        // 2) Reconciliation — kataloqda olub real endpoint dəstində OLMAYAN xəyali icazələri sil
        // (məs. köhnə boolean grid migrasiyasından gələn AUDIT_LOG:DISPATCH, CONFIG:APPROVE_PM).
        // Əvvəlcə rol-link sətirlərini, sonra icazələri sil. Yeganə həqiqət mənbəyi: real @PreAuthorize.
        List<Long> phantomIds = permissionRepository.findAll().stream()
                .filter(p -> !realCodes.contains(p.getCode()))
                .map(Permission::getId)
                .toList();
        if (!phantomIds.isEmpty()) {
            permissionRepository.deleteGrantLinksByPermissionIds(phantomIds);
            permissionRepository.deleteAllById(phantomIds);
        }

        if (created > 0 || !phantomIds.isEmpty()) {
            log.info("İcazə kataloqu sinxronlaşdırıldı: {} yeni, {} xəyali silindi.", created, phantomIds.size());
        }
    }

    private Set<String> collectAuthorityCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (HandlerMethod hm : handlerMapping.getHandlerMethods().values()) {
            extract(hm.getMethodAnnotation(PreAuthorize.class), codes);
            extract(hm.getBeanType().getAnnotation(PreAuthorize.class), codes);
        }
        return codes;
    }

    private void extract(PreAuthorize ann, Set<String> codes) {
        if (ann == null || ann.value() == null) return;
        Matcher m = CODE.matcher(ann.value());
        while (m.find()) codes.add(m.group(1));
    }
}
