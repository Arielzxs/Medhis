package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("billing_record")
public class BillingRecord extends BaseEntity {
    private Long patientId;
    private String billingType;
    private BigDecimal amount;
    private String payChannel;
    private String settlementType;
    private String status;
}
