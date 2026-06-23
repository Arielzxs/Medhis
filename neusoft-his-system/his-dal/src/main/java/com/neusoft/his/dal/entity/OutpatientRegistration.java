package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("outpatient_registration")
public class OutpatientRegistration extends BaseEntity {
    private Long patientId;
    private Long doctorId;
    private String department;
    private String scheduleDate;
    private String status;
    private BigDecimal fee;
    private String paid;
}
