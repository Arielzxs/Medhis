package com.neusoft.his.common.security;

import com.neusoft.his.common.exception.BizException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Set;

/**
 * 基于角色的接口权限拦截器。
 *
 * <p>Controller 或方法上标注 {@link RequireRoles} 后，请求必须携带有效 JWT，
 * 且当前用户至少具备一个声明角色才能继续访问。</p>
 */
public class RbacInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRoles requireRoles = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRoles.class);
        if (requireRoles == null) {
            requireRoles = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRoles.class);
        }
        if (requireRoles == null) {
            return true;
        }
        SecurityUser user = SecurityUserHolder.get();
        if (user == null) {
            throw new BizException("未登录或令牌无效");
        }
        Set<String> roles = user.roles();
        boolean pass = Arrays.stream(requireRoles.value()).anyMatch(roles::contains);
        if (!pass) {
            throw new BizException("无权限访问该接口");
        }
        return true;
    }
}
