package com.neusoft.his.common.audit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.SecurityUser;
import com.neusoft.his.common.security.SecurityUserHolder;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.entity.SysAuditLog;
import com.neusoft.his.dal.mapper.PatientMapper;
import com.neusoft.his.dal.mapper.SysAuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DbAuditServiceImpl implements AuditService {
    private static final Pattern PATIENT_ID_PATTERN = Pattern.compile("(patient=|patient:|患者[:：]\\s*|患者充值[:：]\\s*)(\\d+)");

    private final SysAuditLogMapper auditMapper;
    private final PatientMapper patientMapper;

    public DbAuditServiceImpl(SysAuditLogMapper auditMapper, PatientMapper patientMapper) {
        this.auditMapper = auditMapper;
        this.patientMapper = patientMapper;
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
                .map(this::toEntry)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<AuditLogEntry> page(long page, long size) {
        Page<Object> pageParam = new Page<>(Math.max(page, 1), Math.max(size, 1));
        List<AuditLogEntry> records = auditMapper.selectAuditPage(pageParam).stream()
                .map(this::toEntry)
                .toList();
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), auditMapper.countAuditPage(), records);
    }

    @Override
    public void clear() {
        auditMapper.delete(null);
        log("AUDIT_CLEAR", "清空系统审计日志");
    }

    private AuditLogEntry toEntry(SysAuditLog log) {
        return new AuditLogEntry(log.getTime(), log.getUsername(), log.getOperation(), readableDetail(log.getDetail()));
    }

    private String readableDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return detail;
        }
        Matcher matcher = PATIENT_ID_PATTERN.matcher(detail);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Long patientId = Long.valueOf(matcher.group(2));
            Patient patient = patientMapper.selectById(patientId);
            if (patient == null) {
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + patientLabel(patient)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String patientLabel(Patient patient) {
        String patientNo = patient.getPatientNo() == null || patient.getPatientNo().isBlank()
                ? "ID " + patient.getId()
                : patient.getPatientNo();
        return patient.getName() + "（" + patientNo + "）";
    }
}
