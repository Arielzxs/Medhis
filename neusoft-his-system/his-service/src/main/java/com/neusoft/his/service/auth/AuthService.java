package com.neusoft.his.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.common.security.JwtTokenProvider;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.SysUser;
import com.neusoft.his.dal.entity.SysUserRole;
import com.neusoft.his.dal.mapper.SysUserMapper;
import com.neusoft.his.dal.mapper.SysUserRoleMapper;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper sysUserMapper, SysUserRoleMapper sysUserRoleMapper,
                       AuditService auditService, JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.auditService = auditService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long register(AuthRequest req) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", req.username());
        if (sysUserMapper.selectCount(query) > 0) {
            throw new BizException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setName(req.name());
        user.setEnabled("Y");
        user.setCreatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        auditService.log("REGISTER", "新用户注册: " + user.getUsername());
        return user.getId();
    }

    public LoginResponse login(AuthRequest req) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", req.username());
        SysUser user = sysUserMapper.selectOne(query);

        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (!"Y".equals(user.getEnabled())) {
            throw new BizException("账户已被禁用");
        }

        // 真实查询关联的角色表
        QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("user_id", user.getId());
        Set<String> roles = sysUserRoleMapper.selectList(roleQuery).stream()
                .map(SysUserRole::getRoleCode)
                .collect(Collectors.toSet());

        // 临时保障：如果超级管理员还没分配角色，给予默认最大权限
        if (roles.isEmpty() && "admin".equals(user.getUsername())) {
            roles = Set.of(RoleCode.ADMIN);
        }

        String token = tokenProvider.generate(user.getId(), user.getUsername(), roles);
        auditService.log("LOGIN", "用户登录: " + user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), roles);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Set<String> roles) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 先删除该用户原来的所有旧角色
        QueryWrapper<SysUserRole> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("user_id", userId);
        sysUserRoleMapper.delete(deleteWrapper);

        // 插入新的角色集合
        for (String role : roles) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleCode(role);
            sysUserRoleMapper.insert(userRole);
        }

        auditService.log("ASSIGN_ROLES", "为用户 " + user.getUsername() + " 分配角色: " + roles.toString());
    }
}