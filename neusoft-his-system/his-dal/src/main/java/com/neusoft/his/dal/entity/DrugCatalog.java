package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("drug_catalog")
public class DrugCatalog extends BaseEntity {
    private String code;
    private String name;
    private String unit;
    private BigDecimal price;
    private Integer stock;
    private Integer warningThreshold;
}
