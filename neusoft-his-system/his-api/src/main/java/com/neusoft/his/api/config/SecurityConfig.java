package com.neusoft.his.api.config;

import com.neusoft.his.common.security.JwtTokenProvider;
import com.neusoft.his.common.security.RbacInterceptor;
import com.neusoft.his.common.security.SecurityContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider("12345678901234567890123456789012", 24 * 3600);
    }

    @Bean
    public SecurityContextFilter securityContextFilter(JwtTokenProvider provider) {
        return new SecurityContextFilter(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextFilter securityContextFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(securityContextFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    public RbacInterceptor rbacInterceptor() {
        return new RbacInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor()).addPathPatterns("/api/**");
    }
}
