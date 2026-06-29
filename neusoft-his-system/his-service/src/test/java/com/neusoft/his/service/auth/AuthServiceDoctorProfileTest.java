package com.neusoft.his.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.security.JwtTokenProvider;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.SysUser;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.SysRolePermissionMapper;
import com.neusoft.his.dal.mapper.SysUserMapper;
import com.neusoft.his.dal.mapper.SysUserRoleMapper;
import com.neusoft.his.service.dto.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceDoctorProfileTest {

    @Test
    void register_should_create_pending_doctor_profile_for_doctor_role() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        DoctorProfileMapper doctorProfileMapper = mock(DoctorProfileMapper.class);
        AuditService auditService = mock(AuditService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        });

        AuthService service = new AuthService(
                userMapper, userRoleMapper, rolePermissionMapper, doctorProfileMapper,
                auditService, tokenProvider, passwordEncoder
        );

        Long userId = service.register(new AuthRequest("d001", "123456", "张医生", RoleCode.DOCTOR));

        assertThat(userId).isEqualTo(100L);
        verify(doctorProfileMapper).insert(any(DoctorProfile.class));
        verify(doctorProfileMapper).insert(org.mockito.ArgumentMatchers.argThat((DoctorProfile profile) ->
                profile.getUserId().equals(100L)
                        && profile.getName().equals("张医生")
                        && profile.getDepartment().equals("待分配")
                        && profile.getTitle().equals("医师")
                        && profile.getSpecialty().equals("未填写")
                        && profile.getAttendanceStatus().equals("待完善")
        ));
    }

    @Test
    void createStaff_should_create_pending_doctor_profile_for_doctor_role() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        DoctorProfileMapper doctorProfileMapper = mock(DoctorProfileMapper.class);
        AuditService auditService = mock(AuditService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(200L);
            return 1;
        });

        AuthService service = new AuthService(
                userMapper, userRoleMapper, rolePermissionMapper, doctorProfileMapper,
                auditService, tokenProvider, passwordEncoder
        );

        Long userId = service.createStaff(new AuthRequest("d002", "123456", "李医生", RoleCode.DOCTOR));

        assertThat(userId).isEqualTo(200L);
        verify(doctorProfileMapper).insert(org.mockito.ArgumentMatchers.argThat((DoctorProfile profile) ->
                profile.getUserId().equals(200L)
                        && profile.getName().equals("李医生")
                        && profile.getAttendanceStatus().equals("待完善")
        ));
    }
}
