package com.neusoft.his.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SecurityContextFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityContextFilter.class);
    private final JwtTokenProvider tokenProvider;

    public SecurityContextFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String token = resolveToken(auth);
        if (token != null) {
            try {
                SecurityUserHolder.set(tokenProvider.parse(token));
            } catch (Exception ex) {
                LOGGER.warn("JWT token parse failed: {}", ex.getMessage());
                SecurityUserHolder.clear();
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityUserHolder.clear();
        }
    }

    /**
     * 兼容 Knife4j/Swagger 调试时常见的两种填写方式：
     * 1. Authorization: Bearer xxx
     * 2. Authorization: xxx
     */
    private String resolveToken(String auth) {
        if (auth == null || auth.isBlank()) {
            return null;
        }
        String token = auth.trim();
        while (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token.isBlank() ? null : token;
    }
}
