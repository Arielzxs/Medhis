package com.neusoft.his.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.common.security.JwtTokenProvider;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.SysRolePermission;
import com.neusoft.his.dal.entity.SysUser;
import com.neusoft.his.dal.entity.SysUserRole;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.SysRolePermissionMapper;
import com.neusoft.his.dal.mapper.SysUserMapper;
import com.neusoft.his.dal.mapper.SysUserRoleMapper;
import com.neusoft.his.service.dto.AuthRequest;
import com.neusoft.his.service.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证与权限业务服务。
 *
 * <p>负责账号注册登录、JWT 签发、角色分配和角色权限矩阵维护。
 * 管理端权限变更会同步写入审计日志，便于后续追踪关键操作。</p>
 */
@Service
public class AuthService {
    private static final String PENDING_DEPARTMENT = "待分配";
    private static final String DEFAULT_DOCTOR_TITLE = "医师";
    private static final String DEFAULT_DOCTOR_SPECIALTY = "未填写";
    private static final String PENDING_ATTENDANCE_STATUS = "待完善";

    private static final Set<String> ALL_ROLES = Set.of(
            RoleCode.ADMIN,
            RoleCode.DOCTOR,
            RoleCode.REGISTRAR,
            RoleCode.PHARMACY_ADMIN,
            RoleCode.FINANCE
    );
    private static final Set<String> SELF_REGISTER_ROLES = Set.of(
            RoleCode.DOCTOR,
            RoleCode.REGISTRAR,
            RoleCode.PHARMACY_ADMIN,
            RoleCode.FINANCE
    );
    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "dashboard:view",
            "patient:registration:view",
            "patient:registration:create",
            "patient:tracking:view",
            "doctor:schedule:view",
            "doctor:schedule:manage",
            "doctor:consultation:view",
            "doctor:record:write",
            "doctor:prescription:write",
            "pharmacy:prescription:review",
            "pharmacy:dispense",
            "pharmacy:drug:view",
            "pharmacy:drug:manage",
            "finance:billing:view",
            "finance:billing:manage",
            "analytics:workload:view",
            "analytics:revenue:view",
            "analytics:drug:view",
            "system:user:manage",
            "system:role:manage",
            "system:audit:view"
    );
    private static final Map<String, Set<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
            RoleCode.ADMIN, ALL_PERMISSIONS,
            RoleCode.DOCTOR, Set.of(
                    "dashboard:view",
                    "patient:tracking:view",
                    "doctor:schedule:view",
                    "doctor:consultation:view",
                    "doctor:record:write",
                    "doctor:prescription:write",
                    "analytics:workload:view"
            ),
            RoleCode.REGISTRAR, Set.of(
                    "dashboard:view",
                    "patient:registration:view",
                    "patient:registration:create",
                    "patient:tracking:view",
                    "finance:billing:view"
            ),
            RoleCode.PHARMACY_ADMIN, Set.of(
                    "dashboard:view",
                    "pharmacy:prescription:review",
                    "pharmacy:dispense",
                    "pharmacy:drug:view",
                    "pharmacy:drug:manage",
                    "analytics:drug:view"
            ),
            RoleCode.FINANCE, Set.of(
                    "dashboard:view",
                    "finance:billing:view",
                    "finance:billing:manage",
                    "analytics:revenue:view"
            )
    );

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper sysUserMapper, SysUserRoleMapper sysUserRoleMapper,
                       SysRolePermissionMapper sysRolePermissionMapper, DoctorProfileMapper doctorProfileMapper,
                       AuditService auditService, JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.auditService = auditService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long register(AuthRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()
                || req.name() == null || req.name().isBlank()) {
            throw new BizException("用户名、姓名和密码不能为空");
        }

        String role = req.role() == null ? "" : req.role().trim().toUpperCase();
        if (!SELF_REGISTER_ROLES.contains(role)) {
            throw new BizException("请选择有效的用户身份");
        }

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", req.username().trim());
        if (sysUserMapper.selectCount(query) > 0) {
            throw new BizException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.username().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setName(req.name().trim());
        user.setEnabled("Y");
        user.setCreatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleCode(role);
        sysUserRoleMapper.insert(userRole);

        createPendingDoctorProfileIfNeeded(role, user);
        auditService.log("REGISTER", "新用户注册: " + user.getUsername() + "，角色: " + role);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createStaff(AuthRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()
                || req.name() == null || req.name().isBlank()) {
            throw new BizException("工号、姓名和初始密码不能为空");
        }

        String role = req.role() == null ? "" : req.role().trim().toUpperCase();
        validateRole(role);

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", req.username().trim());
        if (sysUserMapper.selectCount(query) > 0) {
            throw new BizException("职工工号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(req.username().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setName(req.name().trim());
        user.setEnabled("Y");
        user.setCreatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleCode(role);
        sysUserRoleMapper.insert(userRole);

        createPendingDoctorProfileIfNeeded(role, user);
        auditService.log("STAFF_CREATE", "新增职工账号: " + user.getUsername() + "，角色: " + role);
        return user.getId();
    }

    private void createPendingDoctorProfileIfNeeded(String role, SysUser user) {
        if (!RoleCode.DOCTOR.equals(role)) {
            return;
        }
        DoctorProfile profile = new DoctorProfile();
        profile.setUserId(user.getId());
        profile.setName(user.getName());
        profile.setDepartment(PENDING_DEPARTMENT);
        profile.setTitle(DEFAULT_DOCTOR_TITLE);
        profile.setSpecialty(DEFAULT_DOCTOR_SPECIALTY);
        profile.setAttendanceStatus(PENDING_ATTENDANCE_STATUS);
        profile.setCreatedAt(LocalDateTime.now());
        doctorProfileMapper.insert(profile);
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
        if (roles == null || roles.stream().anyMatch(role -> !ALL_ROLES.contains(role))) {
            throw new BizException("包含无效的系统角色");
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

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String password) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        auditService.log("RESET_PASSWORD", "重置用户密码: " + user.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(Long userId, boolean enabled) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setEnabled(enabled ? "Y" : "N");
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        auditService.log("USER_STATUS", (enabled ? "启用" : "停用") + "用户: " + user.getUsername());
    }

    public List<Map<String, Object>> listUsers(String keyword, String role) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.like("username", keyword).or().like("name", keyword);
        }
        query.orderByDesc("created_at");

        return sysUserMapper.selectList(query).stream()
                .map(user -> {
                    QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
                    roleQuery.eq("user_id", user.getId());
                    Set<String> roles = sysUserRoleMapper.selectList(roleQuery).stream()
                            .map(SysUserRole::getRoleCode)
                            .collect(Collectors.toSet());
                    if (roles.isEmpty() && "admin".equals(user.getUsername())) {
                        roles = Set.of(RoleCode.ADMIN);
                    }
                    if (role != null && !role.isBlank() && !roles.contains(role)) {
                        return null;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(user.getId()));
                    item.put("username", user.getUsername());
                    item.put("name", user.getName());
                    item.put("enabled", user.getEnabled());
                    item.put("roles", roles);
                    item.put("createdAt", user.getCreatedAt());
                    return item;
                })
                .filter(item -> item != null)
                .toList();
    }

    public Map<String, Set<String>> rolePermissions() {
        Map<String, Set<String>> matrix = new LinkedHashMap<>();
        ALL_ROLES.forEach(role -> matrix.put(role, loadRolePermissions(role)));
        return matrix;
    }

    public Set<String> rolePermissions(String role) {
        validateRole(role);
        return loadRolePermissions(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(String role, Set<String> permissions) {
        validateRole(role);
        if (permissions == null) {
            throw new BizException("权限配置不能为空");
        }
        if (permissions.stream().anyMatch(permission -> !ALL_PERMISSIONS.contains(permission))) {
            throw new BizException("包含无效的权限点");
        }

        QueryWrapper<SysRolePermission> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("role_code", role);
        sysRolePermissionMapper.delete(deleteWrapper);

        for (String permission : permissions) {
            SysRolePermission item = new SysRolePermission();
            item.setRoleCode(role);
            item.setPermissionCode(permission);
            sysRolePermissionMapper.insert(item);
        }
        auditService.log("ROLE_PERMISSION_SAVE", "角色权限矩阵: " + role + " -> " + permissions);
    }

    public Map<String, Set<String>> defaultRolePermissions() {
        Map<String, Set<String>> defaults = new LinkedHashMap<>();
        ALL_ROLES.forEach(role -> defaults.put(role, new LinkedHashSet<>(DEFAULT_ROLE_PERMISSIONS.getOrDefault(role, Set.of()))));
        return defaults;
    }

    private Set<String> loadRolePermissions(String role) {
        validateRole(role);
        QueryWrapper<SysRolePermission> query = new QueryWrapper<>();
        query.eq("role_code", role);
        Set<String> saved = sysRolePermissionMapper.selectList(query).stream()
                .map(SysRolePermission::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (saved.isEmpty()) {
            return new LinkedHashSet<>(DEFAULT_ROLE_PERMISSIONS.getOrDefault(role, Set.of()));
        }
        return saved;
    }

    private void validateRole(String role) {
        if (role == null || !ALL_ROLES.contains(role)) {
            throw new BizException("无效的系统角色");
        }
    }
}
