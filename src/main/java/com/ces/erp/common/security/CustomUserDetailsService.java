package com.ces.erp.common.security;

import com.ces.erp.permission.entity.Permission;
import com.ces.erp.role.entity.Role;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı: " + email));

        // Tam permission-əsaslı: bütün rolların granted icazə code-larının birləşməsi
        Set<String> codes = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            for (Permission p : role.getGrantedPermissions()) codes.add(p.getCode());
        }

        List<GrantedAuthority> authorities = codes.stream()
                .map(c -> (GrantedAuthority) new SimpleGrantedAuthority(c))
                .toList();

        return new UserPrincipal(user, authorities);
    }
}
