package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("prescription")
public class Prescription extends BaseEntity {
    private Long patientId;
    private Long doctorId;
    private String type;
    private String drugItems;
    private String checkItems;
    private BigDecimal totalAmount;
    private String paid;
    private String auditStatus;
    private String dispenseStatus;
}
