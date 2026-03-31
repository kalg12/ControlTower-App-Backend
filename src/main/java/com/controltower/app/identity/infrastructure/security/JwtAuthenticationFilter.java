package com.controltower.app.identity.infrastructure.security;

import com.controltower.app.identity.application.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the JWT from the Authorization header, validates it, and
 * populates the SecurityContext for the duration of the request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        // Reject MFA-pending tokens on all routes except the 2FA verification endpoint
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isMfaPending(token)
                && !request.getRequestURI().equals("/api/v1/auth/2fa/verify")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)
                && !jwtTokenProvider.isRefreshToken(token)
                && !jwtTokenProvider.isMfaPending(token)) {
            try {
                String userId = jwtTokenProvider.getUserId(token).toString();
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception ex) {
                log.warn("Could not authenticate user from token: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
