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

        // Full-access users: super-admins AND any user with the ADMIN role.
        // In this system the ADMIN role is always created with every permission
        // (see OnboardingService), but role_permissions rows may be missing when
        // new features were added after the tenant was first onboarded.
        // Loading all permissions from the DB directly is the safe fallback.
        boolean fullAccess = user.isSuperAdmin() ||
                user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));

        Set<SimpleGrantedAuthority> authorities;

        if (fullAccess) {
            // Grant every permission in the system + all role codes
            authorities = Stream.concat(
                    permissionRepository.findAll().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getCode())),
                    user.getRoles().stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
            ).collect(Collectors.toSet());
            // Super-admins also get the literal 'super:admin' authority
            // required by cross-tenant endpoints (e.g. supervisor work items)
            if (user.isSuperAdmin()) {
                authorities.add(new SimpleGrantedAuthority("super:admin"));
            }
        } else {
            // Regular users: derive authorities only from assigned role permissions
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
