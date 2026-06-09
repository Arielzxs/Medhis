package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.service.auth.AuthService;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody AuthRequest req) {
        return ApiResponse.ok("注册成功", authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody AuthRequest req) {
        return ApiResponse.ok("登录成功", authService.login(req));
    }

    @PostMapping("/{userId}/roles")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Void> assignRoles(@PathVariable Long userId, @RequestBody Set<String> roles) {
        authService.assignRoles(userId, roles);
        return ApiResponse.ok("角色分配成功", null);
    }
}
