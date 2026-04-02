package com.controltower.app.identity.application;

import com.controltower.app.identity.domain.PermissionRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads UserDetails by user ID (used by JwtAuthenticationFilter).
 * Username here is the user's UUID string.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("User account is not active: " + userId);
        }

        Set<SimpleGrantedAuthority> authorities;

        if (user.isSuperAdmin()) {
            // Super-admins get every permission in the system + ROLE_SUPER_ADMIN
            authorities = Stream.concat(
                    permissionRepository.findAll().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getCode())),
                    Stream.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
            ).collect(Collectors.toSet());
        } else {
            // Build authorities from role codes + permission codes
            authorities = Stream.concat(
                    user.getRoles().stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode())),
                    user.getAllPermissions().stream()
                            .map(SimpleGrantedAuthority::new)
            ).collect(Collectors.toSet());
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
