package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.service.auth.AuthService;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    @PostMapping("/users")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Long> createStaff(@RequestBody AuthRequest req) {
        return ApiResponse.ok("职工账号创建成功", authService.createStaff(req));
    }

    @PostMapping("/{userId}/roles")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Void> assignRoles(@PathVariable Long userId, @RequestBody Set<String> roles) {
        authService.assignRoles(userId, roles);
        return ApiResponse.ok("角色分配成功", null);
    }

    @GetMapping("/users")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<List<Map<String, Object>>> users(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String role) {
        return ApiResponse.ok(authService.listUsers(keyword, role));
    }

    @PutMapping("/users/{userId}/password")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Void> resetPassword(@PathVariable Long userId, @RequestBody Map<String, String> payload) {
        authService.resetPassword(userId, payload == null ? null : payload.get("password"));
        return ApiResponse.ok("密码已重置", null);
    }

    @PutMapping("/users/{userId}/enabled")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Void> updateEnabled(@PathVariable Long userId, @RequestBody Map<String, Boolean> payload) {
        authService.updateEnabled(userId, payload != null && Boolean.TRUE.equals(payload.get("enabled")));
        return ApiResponse.ok("账户状态已更新", null);
    }

    @GetMapping("/role-permissions")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Map<String, Set<String>>> rolePermissions() {
        return ApiResponse.ok(authService.rolePermissions());
    }

    @GetMapping("/role-permissions/defaults")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Map<String, Set<String>>> defaultRolePermissions() {
        return ApiResponse.ok(authService.defaultRolePermissions());
    }

    @GetMapping("/role-permissions/{role}")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Set<String>> rolePermissions(@PathVariable String role) {
        return ApiResponse.ok(authService.rolePermissions(role));
    }

    @PutMapping("/role-permissions/{role}")
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<Void> saveRolePermissions(@PathVariable String role, @RequestBody Set<String> permissions) {
        authService.saveRolePermissions(role, permissions);
        return ApiResponse.ok("角色权限配置已保存", null);
    }
}
