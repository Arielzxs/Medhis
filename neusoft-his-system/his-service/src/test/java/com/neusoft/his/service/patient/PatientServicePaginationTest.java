package com.neusoft.his.service.patient;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PatientMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        AuditService auditService = mock(AuditService.class);
        PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, auditService);

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
