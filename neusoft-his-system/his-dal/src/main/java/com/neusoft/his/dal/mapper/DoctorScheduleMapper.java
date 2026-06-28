package com.neusoft.his.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.view.DoctorScheduleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
    List<DoctorScheduleView> selectSchedulePage(Page<?> page,
                                                @Param("department") String department,
                                                @Param("doctorName") String doctorName,
                                                @Param("date") String date,
                                                @Param("availableOnly") boolean availableOnly);

    long countSchedulePage(@Param("department") String department,
                           @Param("doctorName") String doctorName,
                           @Param("date") String date,
                           @Param("availableOnly") boolean availableOnly);

    List<ScheduleRegistrationCount> countRegistrationsForSchedules(@Param("schedules") List<DoctorScheduleView> schedules);

    record ScheduleRegistrationCount(Long doctorId, String scheduleDate, Long usedCount) {
    }
}
