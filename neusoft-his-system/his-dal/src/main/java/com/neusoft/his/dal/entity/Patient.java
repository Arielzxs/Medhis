package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("patient")
public class Patient extends BaseEntity {

    // 医疗机构患者唯一编号 (数据库要求 Not Null 且 Unique)
    private String patientNo;

    // 患者姓名 (数据库要求 Not Null)
    private String name;

    // 性别，数据库初始值为'未知'
    private String gender = "未知";

    // 出生日期
    private LocalDate birthday;

    // 联系电话号码
    private String phone;

    // 身份证号
    private String idCard;

    // 就诊卡余额
    private BigDecimal balance = BigDecimal.ZERO;

    // 当前就诊阶段状态，数据库初始值为'正常'
    private String currentStatus = "正常";
}
