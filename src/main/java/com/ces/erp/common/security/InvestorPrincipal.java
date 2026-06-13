package com.ces.erp.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Portal (mobil investor tətbiqi) üçün authenticated principal.
 * Şirkət-içi {@link UserPrincipal}-dən tam ayrıdır: heç bir {@code MODULE:ACTION}
 * authority-si daşımır, yalnız {@code ROLE_INVESTOR} — beləcə internal endpoint-lərə 403 alır.
 */
@Getter
public class InvestorPrincipal {

    public static final String ROLE = "ROLE_INVESTOR";

    private final Long investorId;
    private final String accountEmail;

    public InvestorPrincipal(Long investorId, String accountEmail) {
        this.investorId = investorId;
        this.accountEmail = accountEmail;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE));
    }
}
