package com.neusoft.his.dal.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.his.dal.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
    List<SysAuditLog> selectAuditPage(Page<?> page);

    long countAuditPage();
}
