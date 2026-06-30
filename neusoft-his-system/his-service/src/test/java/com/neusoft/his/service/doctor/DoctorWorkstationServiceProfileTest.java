package com.neusoft.his.service.doctor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorLeaveApplication;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.mapper.DoctorLeaveApplicationMapper;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
import com.neusoft.his.dal.mapper.DrugCatalogMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PatientMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import com.neusoft.his.service.dto.DoctorLeaveApplyRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorWorkstationServiceProfileTest {

    @Test
    void listDoctors_should_return_only_active_doctors_for_scheduling() {
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorWorkstationService service = serviceWith(doctorMapper);
        DoctorProfile doctor = new DoctorProfile();
        doctor.setName("张医生");
        doctor.setDepartment("心血管内科");
        doctor.setAttendanceStatus("在岗");
        when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(doctor));

        List<DoctorProfile> doctors = service.listDoctors("心血管内科");

        assertThat(doctors).containsExactly(doctor);
        ArgumentCaptor<QueryWrapper<DoctorProfile>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(doctorMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("attendance_status");
    }

    @Test
    void saveDoctor_should_require_department_for_admin_profile_completion() {
        DoctorWorkstationService service = serviceWith(mock(DoctorProfileMapper.class));
        DoctorProfile profile = new DoctorProfile();
        profile.setName("张医生");
        profile.setTitle("主治医师");
        profile.setAttendanceStatus("在岗");

        assertThatThrownBy(() -> service.saveDoctor(profile))
                .isInstanceOf(BizException.class)
                .hasMessage("请选择医生所属科室");
    }

    @Test
    void updateOwnDoctorProfile_should_update_own_profile_fields() {
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorProfile existing = new DoctorProfile();
        existing.setId(10L);
        existing.setUserId(100L);
        existing.setName("张医生");
        existing.setDepartment("心血管内科");
        existing.setTitle("主任医师");
        existing.setAttendanceStatus("在岗");
        existing.setSpecialty("普通门诊");
        when(doctorMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
        DoctorWorkstationService service = serviceWith(doctorMapper);

        DoctorProfile payload = new DoctorProfile();
        payload.setDepartment("儿科");
        payload.setTitle("主治医师");
        payload.setAttendanceStatus("停诊");
        payload.setSpecialty("高血压、冠心病");

        DoctorProfile updated = service.updateOwnDoctorProfile(100L, payload);

        assertThat(updated.getDepartment()).isEqualTo("儿科");
        assertThat(updated.getTitle()).isEqualTo("主治医师");
        assertThat(updated.getAttendanceStatus()).isEqualTo("在岗");
        assertThat(updated.getSpecialty()).isEqualTo("高血压、冠心病");
        verify(doctorMapper).updateById(argThat((DoctorProfile profile) ->
                profile.getId().equals(10L)
                        && profile.getDepartment().equals("儿科")
                        && profile.getTitle().equals("主治医师")
                        && profile.getAttendanceStatus().equals("在岗")
                        && profile.getSpecialty().equals("高血压、冠心病")
        ));
    }

    @Test
    void submitLeave_should_create_active_leave_and_switch_own_attendance_status_to_rest() {
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorLeaveApplicationMapper leaveMapper = mock(DoctorLeaveApplicationMapper.class);
        DoctorProfile existing = new DoctorProfile();
        existing.setId(10L);
        existing.setUserId(100L);
        existing.setName("张医生");
        existing.setDepartment("心血管内科");
        existing.setTitle("主任医师");
        existing.setAttendanceStatus("在诊");
        existing.setSpecialty("普通门诊");
        when(doctorMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
        when(doctorMapper.selectById(10L)).thenReturn(existing);
        when(leaveMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L, 1L);
        when(leaveMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(activeLeave()));
        DoctorWorkstationService service = serviceWith(doctorMapper, leaveMapper);
        LocalDateTime now = LocalDateTime.now();

        DoctorLeaveApplication leave = service.submitLeave(100L, new DoctorLeaveApplyRequest(
                now.minusMinutes(1), now.plusHours(2), "家中有事"
        ));

        assertThat(leave.getStatus()).isEqualTo("生效中");
        verify(doctorMapper).updateById(argThat((DoctorProfile profile) ->
                profile.getId().equals(10L)
                        && profile.getAttendanceStatus().equals("休息")
        ));
    }

    private DoctorWorkstationService serviceWith(DoctorProfileMapper doctorMapper) {
        return serviceWith(doctorMapper, mock(DoctorLeaveApplicationMapper.class));
    }

    private DoctorWorkstationService serviceWith(DoctorProfileMapper doctorMapper, DoctorLeaveApplicationMapper leaveMapper) {
        return new DoctorWorkstationService(
                doctorMapper,
                leaveMapper,
                mock(DoctorScheduleMapper.class),
                mock(MedicalRecordMapper.class),
                mock(OutpatientRegistrationMapper.class),
                mock(PatientMapper.class),
                mock(PrescriptionMapper.class),
                mock(DrugCatalogMapper.class),
                mock(AuditService.class)
        );
    }

    private DoctorLeaveApplication activeLeave() {
        DoctorLeaveApplication leave = new DoctorLeaveApplication();
        leave.setId(1L);
        leave.setDoctorId(10L);
        leave.setStatus("生效中");
        leave.setStartTime(LocalDateTime.now().minusMinutes(1));
        leave.setEndTime(LocalDateTime.now().plusHours(2));
        return leave;
    }
}
