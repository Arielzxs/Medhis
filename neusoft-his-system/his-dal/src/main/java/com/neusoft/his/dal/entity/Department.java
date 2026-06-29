package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("department")
public class Department extends BaseEntity {
    private String name;
    private String code;
    private String location;
    private String description;
    private Integer sortNo;
    private Integer enabled;
}
