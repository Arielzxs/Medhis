package com.neusoft.his.common.security;

import com.neusoft.his.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Set;

class RbacInterceptorTest {

    private final RbacInterceptor interceptor = new RbacInterceptor();

    @AfterEach
    void tearDown() {
        SecurityUserHolder.clear();
    }

    @Test
    void shouldAllowWhenRoleMatches() throws Exception {
        SecurityUserHolder.set(new SecurityUser(1L, "admin", Set.of(RoleCode.ADMIN)));
        HandlerMethod method = handler("adminOnly");
        boolean result = interceptor.preHandle(request(), response(), method);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldRejectWhenRoleNotMatch() throws Exception {
        SecurityUserHolder.set(new SecurityUser(2L, "doctor", Set.of(RoleCode.DOCTOR)));
        HandlerMethod method = handler("adminOnly");
        Assertions.assertThrows(BizException.class, () -> interceptor.preHandle(request(), response(), method));
    }

    private HandlerMethod handler(String methodName) throws Exception {
        Method method = DemoController.class.getMethod(methodName);
        return new HandlerMethod(new DemoController(), method);
    }

    private HttpServletRequest request() {
        return new org.springframework.mock.web.MockHttpServletRequest();
    }

    private HttpServletResponse response() {
        return new org.springframework.mock.web.MockHttpServletResponse();
    }

    @SuppressWarnings("unused")
    static class DemoController {
        @RequireRoles(RoleCode.ADMIN)
        public void adminOnly() {
        }
    }
}

