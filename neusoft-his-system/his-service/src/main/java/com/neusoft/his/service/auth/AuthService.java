package com.neusoft.his.service.auth;

import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.common.security.JwtTokenProvider;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.SysUser;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuthService {
    private final Map<String, SysUser> users = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userRoles = new ConcurrentHashMap<>();
    private final AtomicLong id = new AtomicLong(1);
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuditService auditService, JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.auditService = auditService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        SysUser admin = new SysUser();
        admin.setId(id.getAndIncrement());
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setName("系统管理员");
        admin.setEnabled("Y");
        admin.setCreatedAt(LocalDateTime.now());
        users.put(admin.getUsername(), admin);
        userRoles.put(admin.getId(), Set.of(RoleCode.ADMIN));
    }

    public Long register(AuthRequest req) {
        if (users.containsKey(req.username())) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setId(id.getAndIncrement());
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setName(req.name());
        user.setEnabled("Y");
        user.setCreatedAt(LocalDateTime.now());
        users.put(user.getUsername(), user);
        userRoles.put(user.getId(), Set.of(RoleCode.REGISTRAR));
        auditService.log("REGISTER", "新用户注册: " + user.getUsername());
        return user.getId();
    }

    public LoginResponse login(AuthRequest req) {
        SysUser user = users.get(req.username());
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        Set<String> roles = userRoles.getOrDefault(user.getId(), Set.of());
        String token = tokenProvider.generate(user.getId(), user.getUsername(), roles);
        auditService.log("LOGIN", "用户登录: " + user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), roles);
    }

    public void assignRoles(Long userId, Set<String> roles) {
        userRoles.put(userId, roles);
        auditService.log("ASSIGN_ROLE", "用户ID=" + userId + " 角色=" + roles);
    }
}
