package com.neusoft.his.common.audit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
}