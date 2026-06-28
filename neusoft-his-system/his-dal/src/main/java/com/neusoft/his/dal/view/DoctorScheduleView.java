package com.neusoft.his.dal.view;

public record DoctorScheduleView(
        Long id,
        Long doctorId,
        String scheduleDate,
        String shift,
        String department,
        String doctorName,
        String name,
        String title,
        String attendanceStatus,
        String level,
        Integer limit,
        Integer remain,
        Integer status
) {
}
