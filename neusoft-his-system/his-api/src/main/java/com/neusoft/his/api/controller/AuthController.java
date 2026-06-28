package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.service.auth.AuthService;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证与权限", description = "登录注册、职工账号、角色授权与角色权限矩阵")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "普通用户自助注册，按岗位生成默认角色。")
    public ApiResponse<Long> register(@RequestBody AuthRequest req) {
        return ApiResponse.ok("注册成功", authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验账号密码并返回 JWT token、用户信息和角色集合。")
    public ApiResponse<LoginResponse> login(@RequestBody AuthRequest req) {
        return ApiResponse.ok("登录成功", authService.login(req));
    }

    @PostMapping("/users")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "创建职工账号", description = "管理员创建院内职工账号。")
    public ApiResponse<Long> createStaff(@RequestBody AuthRequest req) {
        return ApiResponse.ok("职工账号创建成功", authService.createStaff(req));
    }

    @PostMapping("/{userId}/roles")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "分配用户角色", description = "管理员为指定用户覆盖保存角色集合。")
    public ApiResponse<Void> assignRoles(@Parameter(description = "用户 ID") @PathVariable Long userId,
                                         @RequestBody Set<String> roles) {
        authService.assignRoles(userId, roles);
        return ApiResponse.ok("角色分配成功", null);
    }

    @GetMapping("/users")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询用户列表", description = "支持按关键字和角色筛选系统账号。")
    public ApiResponse<List<Map<String, Object>>> users(@Parameter(description = "用户名或姓名关键字") @RequestParam(required = false) String keyword,
                                                        @Parameter(description = "角色编码") @RequestParam(required = false) String role) {
        return ApiResponse.ok(authService.listUsers(keyword, role));
    }

    @PutMapping("/users/{userId}/password")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "重置用户密码", description = "管理员为指定用户设置新密码。")
    public ApiResponse<Void> resetPassword(@Parameter(description = "用户 ID") @PathVariable Long userId,
                                           @RequestBody Map<String, String> payload) {
        authService.resetPassword(userId, payload == null ? null : payload.get("password"));
        return ApiResponse.ok("密码已重置", null);
    }

    @PutMapping("/users/{userId}/enabled")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "启用或禁用账号", description = "管理员更新用户账号可用状态。")
    public ApiResponse<Void> updateEnabled(@Parameter(description = "用户 ID") @PathVariable Long userId,
                                           @RequestBody Map<String, Boolean> payload) {
        authService.updateEnabled(userId, payload != null && Boolean.TRUE.equals(payload.get("enabled")));
        return ApiResponse.ok("账户状态已更新", null);
    }

    @GetMapping("/role-permissions")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询角色权限矩阵", description = "返回所有角色当前配置的权限编码集合。")
    public ApiResponse<Map<String, Set<String>>> rolePermissions() {
        return ApiResponse.ok(authService.rolePermissions());
    }

    @GetMapping("/role-permissions/defaults")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询默认角色权限", description = "返回系统内置的角色权限模板。")
    public ApiResponse<Map<String, Set<String>>> defaultRolePermissions() {
        return ApiResponse.ok(authService.defaultRolePermissions());
    }

    @GetMapping("/role-permissions/{role}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询单个角色权限", description = "按角色编码返回该角色权限集合。")
    public ApiResponse<Set<String>> rolePermissions(@Parameter(description = "角色编码") @PathVariable String role) {
        return ApiResponse.ok(authService.rolePermissions(role));
    }

    @PutMapping("/role-permissions/{role}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "保存角色权限", description = "覆盖保存指定角色的权限编码集合。")
    public ApiResponse<Void> saveRolePermissions(@Parameter(description = "角色编码") @PathVariable String role,
                                                 @RequestBody Set<String> permissions) {
        authService.saveRolePermissions(role, permissions);
        return ApiResponse.ok("角色权限配置已保存", null);
    }
}
