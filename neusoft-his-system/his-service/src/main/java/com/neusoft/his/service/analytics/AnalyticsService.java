package com.neusoft.his.service.analytics;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.mapper.FinancialTransactionMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final OutpatientRegistrationMapper registrationMapper;
    private final MedicalRecordMapper recordMapper;
    private final FinancialTransactionMapper transactionMapper;
    private final AuditService auditService;

    public AnalyticsService(OutpatientRegistrationMapper registrationMapper, MedicalRecordMapper recordMapper,
                            FinancialTransactionMapper transactionMapper, AuditService auditService) {
        this.registrationMapper = registrationMapper;
        this.recordMapper = recordMapper;
        this.transactionMapper = transactionMapper;
        this.auditService = auditService;
    }

    public Map<String, Long> outpatientTrend() {
        List<OutpatientRegistration> list = registrationMapper.selectList(new QueryWrapper<>());
        return list.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));
    }

    public Map<String, Long> doctorWorkload() {
        List<MedicalRecord> list = recordMapper.selectList(new QueryWrapper<>());
        return list.stream()
                .filter(r -> r.getDoctorId() != null)
                .collect(Collectors.groupingBy(
                        r -> "Doctor_ID_" + r.getDoctorId(),
                        Collectors.counting()
                ));
    }

    public String financeSummary() {
        List<FinancialTransaction> records = transactionMapper.selectList(new QueryWrapper<>());
        double in = records.stream().filter(r -> "IN".equals(r.getDirection())).mapToDouble(r -> r.getAmount().doubleValue()).sum();
        double out = records.stream().filter(r -> "OUT".equals(r.getDirection())).mapToDouble(r -> r.getAmount().doubleValue()).sum();
        return "财务报表: 总收入=" + in + ", 总支出=" + out + ", 总结余=" + (in - out);
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "outpatientTrend", outpatientTrend(),
                "doctorWorkload", doctorWorkload(),
                "financeSummary", financeSummary(),
                "auditLogCount", auditService.list().size() // 演示保留
        );
    }
}