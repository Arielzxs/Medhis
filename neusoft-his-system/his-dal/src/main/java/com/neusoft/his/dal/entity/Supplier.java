package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("supplier")
public class Supplier extends BaseEntity {
    private String name;
    private String contact;
    private String phone;
    private String qualification;
}
