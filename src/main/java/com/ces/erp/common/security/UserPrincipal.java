package com.ces.erp.common.security;

import com.ces.erp.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean active;
    private final boolean hasApproval;
    private final String roleName;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.hasApproval = user.isHasApproval();
        this.roleName = user.getRole() != null ? user.getRole().getName() : null;

        // Hər modul üçün icazələri GrantedAuthority-yə çevir
        // Format: "MODULE_CODE:ACTION"  →  örn: "CUSTOMER_MANAGEMENT:GET"
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            this.authorities = user.getRole().getPermissions().stream()
                    .flatMap(p -> {
                        String code = p.getModule().getCode();
                        return Stream.of(
                                p.isCanGet()               ? new SimpleGrantedAuthority(code + ":GET")                : null,
                                p.isCanPost()              ? new SimpleGrantedAuthority(code + ":POST")               : null,
                                p.isCanPut()               ? new SimpleGrantedAuthority(code + ":PUT")                : null,
                                p.isCanDelete()            ? new SimpleGrantedAuthority(code + ":DELETE")             : null,
                                p.isCanSendToCoordinator() ? new SimpleGrantedAuthority(code + ":SEND_COORDINATOR")   : null,
                                p.isCanSubmitOffer()       ? new SimpleGrantedAuthority(code + ":SUBMIT_OFFER")       : null,
                                p.isCanSendToAccounting()  ? new SimpleGrantedAuthority(code + ":SEND_ACCOUNTING")    : null,
                                p.isCanReturnToProject()   ? new SimpleGrantedAuthority(code + ":RETURN_PROJECT")     : null,
                                p.isCanApproveByPm()       ? new SimpleGrantedAuthority(code + ":APPROVE_PM")         : null,
                                p.isCanCheckDocuments()    ? new SimpleGrantedAuthority(code + ":CHECK_DOCUMENTS")    : null,
                                p.isCanDispatch()          ? new SimpleGrantedAuthority(code + ":DISPATCH")           : null,
                                p.isCanDeliver()           ? new SimpleGrantedAuthority(code + ":DELIVER")            : null
                        ).filter(a -> a != null);
                    })
                    .toList();
        } else {
            this.authorities = List.of();
        }
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                      { return password; }
    @Override public String getUsername()                                      { return email; }
    @Override public boolean isAccountNonExpired()                             { return true; }
    @Override public boolean isAccountNonLocked()                              { return active; }
    @Override public boolean isCredentialsNonExpired()                         { return true; }
    @Override public boolean isEnabled()                                       { return active; }
}
