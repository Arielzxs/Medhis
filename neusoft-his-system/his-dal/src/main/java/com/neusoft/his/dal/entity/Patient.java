package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("patient")
public class Patient extends BaseEntity {
    private String patientNo;
    private String name;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String idCard;
    private String currentStatus;
}
