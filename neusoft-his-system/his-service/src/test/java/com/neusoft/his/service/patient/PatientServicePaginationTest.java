package com.neusoft.his.service.patient;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.mapper.DoctorLeaveApplicationMapper;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PatientMapper;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientServicePaginationTest {

    @Test
    void registrations_should_batch_load_patients_and_doctors_for_current_page_only() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorLeaveApplicationMapper leaveMapper = mock(DoctorLeaveApplicationMapper.class);
        AuditService auditService = mock(AuditService.class);
        PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, scheduleMapper, leaveMapper, auditService);

        OutpatientRegistration reg = new OutpatientRegistration();
        reg.setId(101L);
        reg.setPatientId(201L);
        reg.setDoctorId(301L);
        reg.setDepartment("儿科");
        reg.setStatus("待诊");
        reg.setPaid("Y");
        reg.setScheduleDate("2026-06-28");
        reg.setFee(BigDecimal.TEN);
        reg.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 0));

        when(registrationMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            Page<OutpatientRegistration> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(reg));
            return page;
        });
        when(patientMapper.selectBatchIds(List.of(201L))).thenReturn(List.of(patient(201L, "李雷")));
        when(doctorMapper.selectBatchIds(List.of(301L))).thenReturn(List.of(doctor(301L, "王医生")));

        PageResponse<Map<String, Object>> response = service.registrations(null, "待诊", 1, 10);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).singleElement().satisfies(item -> {
            assertThat(item.get("patientName")).isEqualTo("李雷");
            assertThat(item.get("doctorName")).isEqualTo("王医生");
        });
        verify(patientMapper).selectBatchIds(List.of(201L));
        verify(doctorMapper).selectBatchIds(List.of(301L));
        verify(patientMapper, never()).selectById(201L);
        verify(doctorMapper, never()).selectById(301L);
    }

    @Test
    void register_should_reject_resting_doctor_even_if_client_submits_old_schedule() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorLeaveApplicationMapper leaveMapper = mock(DoctorLeaveApplicationMapper.class);
        AuditService auditService = mock(AuditService.class);
        PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, scheduleMapper, leaveMapper, auditService);
        DoctorProfile doctor = doctor(301L, "王医生");
        doctor.setAttendanceStatus("休息");

        when(patientMapper.selectById(201L)).thenReturn(patient(201L, "李雷"));
        when(doctorMapper.selectById(301L)).thenReturn(doctor);

        assertThatThrownBy(() -> service.register(new PatientRegistrationRequest(
                201L, 301L, "儿科", "2026-06-29", "上午", BigDecimal.TEN
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("休息");
        verify(registrationMapper, never()).insert(any(OutpatientRegistration.class));
        verify(scheduleMapper, never()).selectList(any(QueryWrapper.class));
    }

    @Test
    void register_should_reject_when_doctor_leave_overlaps_selected_shift() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorLeaveApplicationMapper leaveMapper = mock(DoctorLeaveApplicationMapper.class);
        AuditService auditService = mock(AuditService.class);
        PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, scheduleMapper, leaveMapper, auditService);
        DoctorProfile doctor = doctor(301L, "王医生");
        doctor.setAttendanceStatus("在诊");
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(301L);
        schedule.setScheduleDate("2026-06-29");
        schedule.setShift("上午");
        schedule.setRegistrationLimit(10);

        when(patientMapper.selectById(201L)).thenReturn(patient(201L, "李雷"));
        when(doctorMapper.selectById(301L)).thenReturn(doctor);
        when(scheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(schedule));
        when(leaveMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.register(new PatientRegistrationRequest(
                201L, 301L, "儿科", "2026-06-29", "上午", BigDecimal.TEN
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请假记录");
        verify(registrationMapper, never()).insert(any(OutpatientRegistration.class));
    }

    @Test
    void payRegistration_should_deduct_patient_balance_and_mark_paid() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorLeaveApplicationMapper leaveMapper = mock(DoctorLeaveApplicationMapper.class);
        AuditService auditService = mock(AuditService.class);
        PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, scheduleMapper, leaveMapper, auditService);
        Patient patient = patient(201L, "李雷");
        patient.setBalance(new BigDecimal("100.00"));
        OutpatientRegistration reg = new OutpatientRegistration();
        reg.setId(101L);
        reg.setPatientId(201L);
        reg.setFee(new BigDecimal("20.00"));
        reg.setPaid("N");
        reg.setStatus("待缴费");

        when(registrationMapper.selectById(101L)).thenReturn(reg);
        when(patientMapper.selectById(201L)).thenReturn(patient);

        Patient paidPatient = service.payRegistration(101L);

        assertThat(paidPatient.getBalance()).isEqualByComparingTo("80.00");
        assertThat(paidPatient.getCurrentStatus()).isEqualTo("待诊");
        assertThat(reg.getPaid()).isEqualTo("Y");
        assertThat(reg.getStatus()).isEqualTo("待诊");
        verify(registrationMapper).updateById(reg);
        verify(patientMapper).updateById(patient);
    }

    private Patient patient(Long id, String name) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(name);
        return patient;
    }

    private DoctorProfile doctor(Long id, String name) {
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(id);
        doctor.setName(name);
        return doctor;
    }
}
