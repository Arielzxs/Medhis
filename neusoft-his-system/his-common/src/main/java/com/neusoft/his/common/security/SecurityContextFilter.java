package com.neusoft.his.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SecurityContextFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;

    public SecurityContextFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                SecurityUserHolder.set(tokenProvider.parse(auth.substring(7)));
            } catch (Exception ignored) {
                SecurityUserHolder.clear();
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityUserHolder.clear();
        }
    }
}
