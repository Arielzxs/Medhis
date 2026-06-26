package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("doctor_schedule")
public class DoctorSchedule extends BaseEntity {
    private Long doctorId;
    private String scheduleDate;
    private String shift;
    private String level;
    private Integer registrationLimit;
    private Integer status;
}
