package com.neusoft.his.service.doctor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.dal.view.DoctorScheduleView;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DoctorWorkstationServicePaginationTest {

    @Test
    void scheduleQuery_should_delegate_filter_and_paging_to_mapper() {
        DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
        DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
        MedicalRecordMapper recordMapper = mock(MedicalRecordMapper.class);
        OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
        PrescriptionMapper prescriptionMapper = mock(PrescriptionMapper.class);
        AuditService auditService = mock(AuditService.class);
        DoctorWorkstationService service = new DoctorWorkstationService(
                doctorMapper, scheduleMapper, recordMapper, registrationMapper, prescriptionMapper, auditService
        );

        DoctorScheduleView view = new DoctorScheduleView(
                1L, 11L, "2026-06-28", "上午", "心血管内科", "张医生", "张医生",
                "主任医师", "上午", "专家号", 20, 6, 1
        );
        when(scheduleMapper.selectSchedulePage(any(Page.class), eq("心血管内科"), eq("张"), eq("2026-06-28")))
                .thenReturn(List.of(view));
        when(scheduleMapper.countSchedulePage("心血管内科", "张", "2026-06-28")).thenReturn(1L);

        PageResponse<DoctorScheduleView> response = service.scheduleQuery("心血管内科", "张", "2026-06-28", 2, 5);

        assertThat(response.records()).containsExactly(view);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        verify(scheduleMapper).selectSchedulePage(any(Page.class), eq("心血管内科"), eq("张"), eq("2026-06-28"));
        verify(scheduleMapper).countSchedulePage("心血管内科", "张", "2026-06-28");
        verifyNoInteractions(doctorMapper, registrationMapper);
    }
}
