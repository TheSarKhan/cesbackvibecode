package com.ces.erp.common.security;

import com.ces.erp.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean active;
    private final boolean hasApproval;
    private final String roleName; // convenience — ilk rolun adı (display/geriyə uyğunluq)
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Authority-lər {@code CustomUserDetailsService}-də hesablanır:
     * rolların granted icazə code-larının birləşməsi (tam permission-əsaslı, xüsusi hal yox).
     */
    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.hasApproval = user.isHasApproval();
        this.roleName = user.getRoles().stream().findFirst().map(r -> r.getName()).orElse(null);
        this.authorities = authorities;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()              { return password; }
    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}
