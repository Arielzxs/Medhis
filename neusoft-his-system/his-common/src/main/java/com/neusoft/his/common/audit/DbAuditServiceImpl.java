package com.neusoft.his.common.audit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.SecurityUser;
import com.neusoft.his.common.security.SecurityUserHolder;
import com.neusoft.his.dal.entity.SysAuditLog;
import com.neusoft.his.dal.mapper.SysAuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DbAuditServiceImpl implements AuditService {
    private final SysAuditLogMapper auditMapper;

    public DbAuditServiceImpl(SysAuditLogMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @Override
    public void log(String operation, String detail) {
        SecurityUser user = SecurityUserHolder.get();
        SysAuditLog log = new SysAuditLog();
        log.setTime(LocalDateTime.now());
        log.setUsername(user == null ? "anonymous" : user.username());
        log.setOperation(operation);
        log.setDetail(detail);
        auditMapper.insert(log);
    }

    @Override
    public List<AuditLogEntry> list() {
        QueryWrapper<SysAuditLog> query = new QueryWrapper<>();
        query.orderByDesc("time");
        return auditMapper.selectList(query).stream()
                .map(log -> new AuditLogEntry(log.getTime(), log.getUsername(), log.getOperation(), log.getDetail()))
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<AuditLogEntry> page(long page, long size) {
        Page<Object> pageParam = new Page<>(Math.max(page, 1), Math.max(size, 1));
        List<AuditLogEntry> records = auditMapper.selectAuditPage(pageParam).stream()
                .map(log -> new AuditLogEntry(log.getTime(), log.getUsername(), log.getOperation(), log.getDetail()))
                .toList();
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), auditMapper.countAuditPage(), records);
    }

    @Override
    public void clear() {
        auditMapper.delete(null);
        log("AUDIT_CLEAR", "清空系统审计日志");
    }
}
