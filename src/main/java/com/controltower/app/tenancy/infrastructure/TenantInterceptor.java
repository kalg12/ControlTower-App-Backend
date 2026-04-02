package com.controltower.app.tenancy.infrastructure;

import com.controltower.app.identity.infrastructure.security.JwtTokenProvider;
import com.controltower.app.tenancy.domain.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves the active tenant from the authenticated JWT and stores it in
 * TenantContext for the duration of the request.
 *
 * Must run after JwtAuthenticationFilter populates the SecurityContext.
 * Clears TenantContext in afterCompletion to prevent ThreadLocal leaks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            String bearerToken = extractToken(request);
            if (StringUtils.hasText(bearerToken)) {
                try {
                    java.util.UUID tenantId = jwtTokenProvider.getTenantId(bearerToken);
                    TenantContext.setTenantId(tenantId);
                    log.debug("Tenant context set: {}", tenantId);
                } catch (Exception ex) {
                    log.warn("Could not extract tenant from token: {}", ex.getMessage());
                    try {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                                "{\"success\":false,\"message\":\"Invalid or missing tenant in access token\"}");
                    } catch (IOException ioe) {
                        log.error("Could not write 401 response body", ioe);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        TenantContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
