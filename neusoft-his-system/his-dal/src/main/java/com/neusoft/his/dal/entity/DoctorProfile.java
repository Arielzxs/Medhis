package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("doctor_profile")
public class DoctorProfile extends BaseEntity {
    private Long userId;
    private String name;
    private String department;
    private String title;
    private String specialty;
    private String attendanceStatus;
}
