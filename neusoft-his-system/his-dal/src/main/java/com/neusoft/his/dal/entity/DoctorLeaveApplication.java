package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("doctor_leave_application")
public class DoctorLeaveApplication extends BaseEntity {
    private Long doctorId;
    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String status;
    private String previousStatus;
}
