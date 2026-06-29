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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计分析业务服务。
 *
 * <p>面向首页看板和统计报表，汇总门诊量、医生工作量、收入支出和库存预警等运营指标。</p>
 */
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
        LocalDate today = LocalDate.now();
        QueryWrapper<OutpatientRegistration> registrationQuery = new QueryWrapper<>();
        registrationQuery.likeRight("created_at", today.toString());
        List<OutpatientRegistration> todayRegistrations = registrationMapper.selectList(registrationQuery);

        QueryWrapper<FinancialTransaction> financeQuery = new QueryWrapper<>();
        financeQuery.likeRight("created_at", today.toString()).eq("direction", "IN");
        double todayIncome = transactionMapper.selectList(financeQuery).stream()
                .mapToDouble(item -> item.getAmount() == null ? 0D : item.getAmount().doubleValue())
                .sum();

        return Map.of(
                "outpatientTrend", outpatientTrend(),
                "doctorWorkload", doctorWorkload(),
                "financeSummary", financeSummary(),
                "todayRegistrations", todayRegistrations.size(),
                "waitingPatients", todayRegistrations.stream()
                        .filter(item -> item.getStatus() != null && List.of("待缴费", "待诊", "待诊中").contains(item.getStatus()))
                        .count(),
                "todayIncome", todayIncome,
                "auditLogCount", auditService.list().size() // 演示保留
        );
    }
}
