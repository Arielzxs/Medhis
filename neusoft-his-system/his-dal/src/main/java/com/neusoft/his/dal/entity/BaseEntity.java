package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
