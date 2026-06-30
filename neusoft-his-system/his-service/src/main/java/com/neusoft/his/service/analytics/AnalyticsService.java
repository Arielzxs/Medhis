package com.neusoft.his.service.analytics;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DrugCatalogMapper;
import com.neusoft.his.dal.mapper.FinancialTransactionMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    private final DoctorProfileMapper doctorMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final DrugCatalogMapper drugMapper;
    private final FinancialTransactionMapper transactionMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsService(OutpatientRegistrationMapper registrationMapper, MedicalRecordMapper recordMapper,
                            DoctorProfileMapper doctorMapper, PrescriptionMapper prescriptionMapper,
                            DrugCatalogMapper drugMapper, FinancialTransactionMapper transactionMapper,
                            AuditService auditService) {
        this.registrationMapper = registrationMapper;
        this.recordMapper = recordMapper;
        this.doctorMapper = doctorMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.drugMapper = drugMapper;
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

    public List<Map<String, Object>> doctorWorkloadRanking(String department, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<DoctorProfile> doctorQuery = new QueryWrapper<>();
        if (StringUtils.isNotBlank(department)) {
            doctorQuery.eq("department", department.trim());
        }
        List<DoctorProfile> doctors = doctorMapper.selectList(doctorQuery);
        Map<Long, DoctorProfile> doctorMap = doctors.stream()
                .filter(doctor -> doctor.getId() != null)
                .collect(Collectors.toMap(DoctorProfile::getId, Function.identity(), (left, right) -> left));
        Set<Long> doctorIds = doctorMap.keySet();
        if (doctorIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> patientCounts = recordMapper.selectList(dateRangeQuery(startDate, endDate))
                .stream()
                .filter(record -> record.getDoctorId() != null && doctorIds.contains(record.getDoctorId()))
                .collect(Collectors.groupingBy(MedicalRecord::getDoctorId, Collectors.counting()));

        Map<Long, Long> prescriptionCounts = prescriptionMapper.selectList(dateRangeQuery(startDate, endDate))
                .stream()
                .filter(prescription -> prescription.getDoctorId() != null && doctorIds.contains(prescription.getDoctorId()))
                .collect(Collectors.groupingBy(Prescription::getDoctorId, Collectors.counting()));

        return doctors.stream()
                .map(doctor -> workloadRow(doctor, patientCounts, prescriptionCounts))
                .filter(row -> ((Long) row.get("patientCount")) > 0 || ((Long) row.get("prescriptionCount")) > 0)
                .sorted(Comparator
                        .comparingLong((Map<String, Object> row) -> (Long) row.get("patientCount")).reversed()
                        .thenComparing(row -> Objects.toString(row.get("doctorName"), "")))
                .toList();
    }

    public List<Map<String, Object>> drugConsumptionRanking(String category, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<DrugCatalog> drugQuery = new QueryWrapper<>();
        if (StringUtils.isNotBlank(category)) {
            drugQuery.eq("category", category.trim());
        }
        List<DrugCatalog> drugs = drugMapper.selectList(drugQuery);
        Map<Long, DrugCatalog> drugMap = drugs.stream()
                .filter(drug -> drug.getId() != null)
                .collect(Collectors.toMap(DrugCatalog::getId, Function.identity(), (left, right) -> left));
        if (drugMap.isEmpty()) {
            return List.of();
        }

        QueryWrapper<Prescription> prescriptionQuery = dateRangeQuery(startDate, endDate);
        prescriptionQuery.eq("dispense_status", "DONE");
        Map<Long, Long> consumptionMap = new HashMap<>();
        prescriptionMapper.selectList(prescriptionQuery).forEach(prescription ->
                parseDrugItems(prescription.getDrugItems()).forEach(item -> {
                    Long drugId = toLong(item.get("drugId"));
                    if (drugId != null && drugMap.containsKey(drugId)) {
                        Long quantity = toLong(item.get("quantity"));
                        consumptionMap.merge(drugId, Math.max(quantity == null ? 1L : quantity, 0L), Long::sum);
                    }
                })
        );

        return consumptionMap.entrySet().stream()
                .map(entry -> drugConsumptionRow(drugMap.get(entry.getKey()), entry.getValue()))
                .filter(row -> row.get("drugCode") != null)
                .sorted(Comparator
                        .comparingLong((Map<String, Object> row) -> (Long) row.get("consumption")).reversed()
                        .thenComparing(row -> Objects.toString(row.get("drugName"), "")))
                .toList();
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

    private <T> QueryWrapper<T> dateRangeQuery(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<T> query = new QueryWrapper<>();
        if (startDate != null) {
            query.ge("created_at", startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.lt("created_at", endDate.plusDays(1).atStartOfDay());
        }
        return query;
    }

    private Map<String, Object> workloadRow(DoctorProfile doctor, Map<Long, Long> patientCounts,
                                            Map<Long, Long> prescriptionCounts) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("doctorId", doctor.getId());
        row.put("department", StringUtils.defaultIfBlank(doctor.getDepartment(), "未分科"));
        row.put("doctorName", StringUtils.defaultIfBlank(doctor.getName(), "医生#" + doctor.getId()));
        row.put("patientCount", patientCounts.getOrDefault(doctor.getId(), 0L));
        row.put("prescriptionCount", prescriptionCounts.getOrDefault(doctor.getId(), 0L));
        return row;
    }

    private List<Map<String, Object>> parseDrugItems(String drugItems) {
        if (StringUtils.isBlank(drugItems)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(drugItems, new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> drugConsumptionRow(DrugCatalog drug, Long consumption) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (drug == null) {
            return row;
        }
        row.put("drugId", drug.getId());
        row.put("drugCode", drug.getCode());
        row.put("drugName", drug.getName());
        row.put("category", StringUtils.defaultIfBlank(drug.getCategory(), "未分类"));
        row.put("consumption", consumption == null ? 0L : consumption);
        return row;
    }
}
