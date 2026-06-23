package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("financial_transaction")
public class FinancialTransaction extends BaseEntity {
    private String bizType;
    private BigDecimal amount;
    private String direction;
    private String status;
    private String remark;
}
