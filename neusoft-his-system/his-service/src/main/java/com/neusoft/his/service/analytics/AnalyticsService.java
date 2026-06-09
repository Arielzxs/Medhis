package com.neusoft.his.service.analytics;

import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.audit.AuditLogEntry;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.service.finance.FinanceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final AuditService auditService;
    private final FinanceService financeService;

    public AnalyticsService(AuditService auditService, FinanceService financeService) {
        this.auditService = auditService;
        this.financeService = financeService;
    }

    public Map<String, Long> outpatientTrend() {
        return auditService.list().stream()
                .filter(l -> "REGISTRATION".equals(l.operation()))
                .collect(Collectors.groupingBy(l -> l.time().toLocalDate().toString(), Collectors.counting()));
    }

    public Map<String, Long> doctorWorkload() {
        return auditService.list().stream()
                .filter(l -> "CALL_PATIENT".equals(l.operation()))
                .collect(Collectors.groupingBy(AuditLogEntry::username, Collectors.counting()));
    }

    public List<Map.Entry<String, Long>> drugConsumptionRanking() {
        return auditService.list().stream()
                .filter(l -> "DISPENSE".equals(l.operation()))
                .collect(Collectors.groupingBy(AuditLogEntry::detail, Collectors.counting()))
                .entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList();
    }

    public String financeSummary() {
        List<FinancialTransaction> records = financeService.report(1, Long.MAX_VALUE).records();
        double in = records.stream().filter(r -> "IN".equals(r.getDirection())).mapToDouble(r -> r.getAmount().doubleValue()).sum();
        double out = records.stream().filter(r -> "OUT".equals(r.getDirection())).mapToDouble(r -> r.getAmount().doubleValue()).sum();
        return "财务报表: 收入=" + in + ", 支出=" + out + ", 结余=" + (in - out);
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "outpatientTrend", outpatientTrend(),
                "doctorWorkload", doctorWorkload(),
                "drugConsumptionRanking", drugConsumptionRanking(),
                "financeSummary", financeSummary(),
                "auditSize", auditService.list().size()
        );
    }
}
