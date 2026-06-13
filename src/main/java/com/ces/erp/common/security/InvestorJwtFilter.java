package com.ces.erp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Investor portal token-ini emal edir. YALNIZ {@code type="INVESTOR"} token-lərə baxır;
 * digər (USER) token-lərə toxunmadan ötürür — onları {@link JwtAuthFilter} emal edir.
 * Beləcə investor token-i heç vaxt internal {@code loadUserByUsername} yoluna düşmür.
 * <p>
 * KRİTİK izolyasiya: investor principal-i YALNIZ {@code /api/portal/**} yolunda qurulur.
 * Beləcə investor token-i digər internal endpoint-lərə (məs. {@code isAuthenticated()}
 * olanlar) authenticated kimi düşmür → 401 alır. Investor token YALNIZ portal-a keçir.
 */
@Component
@RequiredArgsConstructor
public class InvestorJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // İnvestor principal-i yalnız portal yolunda qurulur (çapraz-izolyasiya)
        if (!request.getRequestURI().startsWith("/api/portal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Token etibarsızdırsa və ya investor tipində deyilsə — bu filter ona baxmır
        if (!jwtUtil.isTokenValid(token) || !"INVESTOR".equals(jwtUtil.extractType(token))) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Long investorId = jwtUtil.extractInvestorId(token);
            String email = jwtUtil.extractEmail(token);
            if (investorId != null) {
                InvestorPrincipal principal = new InvestorPrincipal(investorId, email);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
