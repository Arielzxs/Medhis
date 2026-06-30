package com.neusoft.his.service.finance;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.api.PageSupport;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.BillingRecord;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.BillingRecordMapper;
import com.neusoft.his.dal.mapper.FinancialTransactionMapper;
import com.neusoft.his.dal.mapper.PatientMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 财务管理业务服务。
 *
 * <p>负责处方划价、账单收费、退费、报销、支出登记和财务报表查询。
 * 所有资金方向明确的动作都会写入财务流水和审计日志。</p>
 */
@Service
public class FinanceService {
    private static final Pattern PATIENT_ID_PATTERN = Pattern.compile("(patient=|patient:|患者[:：]\\s*|患者充值[:：]\\s*)(\\d+)");

    private final BillingRecordMapper billingMapper;
    private final FinancialTransactionMapper transactionMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PatientMapper patientMapper;
    private final AuditService auditService;

    public FinanceService(BillingRecordMapper billingMapper, FinancialTransactionMapper transactionMapper,
                          PrescriptionMapper prescriptionMapper, PatientMapper patientMapper, AuditService auditService) {
        this.billingMapper = billingMapper;
        this.transactionMapper = transactionMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.patientMapper = patientMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public BillingRecord pricePrescription(Long prescriptionId) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) throw new BizException("处方不存在");

        BillingRecord billing = new BillingRecord();
        billing.setPatientId(p.getPatientId());
        billing.setBillingType("PRESCRIPTION");
        billing.setAmount(p.getTotalAmount());
        billing.setStatus("PRICED");
        billing.setCreatedAt(LocalDateTime.now());
        billingMapper.insert(billing);

        auditService.log("PRICE", "处方划价 bill=" + billing.getId());
        return billing;
    }

    @Transactional(rollbackFor = Exception.class)
    public BillingRecord charge(Long billId, String channel, String settlementType) {
        BillingRecord bill = billingMapper.selectById(billId);
        if (bill == null) throw new BizException("账单不存在");

        bill.setPayChannel(channel);
        bill.setSettlementType(settlementType);
        bill.setStatus("PAID");
        bill.setUpdatedAt(LocalDateTime.now());
        billingMapper.updateById(bill);

        FinancialTransaction tx = new FinancialTransaction();
        tx.setBizType("OUTPATIENT_CHARGE");
        tx.setDirection("IN");
        tx.setAmount(bill.getAmount());
        tx.setStatus("DONE");
        tx.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);

        auditService.log("CHARGE", "门诊收费 bill=" + billId + " channel=" + channel);
        return bill;
    }

    @Transactional(rollbackFor = Exception.class)
    public FinancialTransaction expense(BigDecimal amount, String remark) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setBizType("PURCHASE_EXPENSE");
        tx.setDirection("OUT");
        tx.setAmount(amount);
        tx.setStatus("DONE");
        tx.setRemark(remark);
        tx.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);
        auditService.log("EXPENSE", "采购支出 amount=" + amount);
        return tx;
    }

    @Transactional(rollbackFor = Exception.class)
    public FinancialTransaction refund(Long billId, String reason) {
        BillingRecord bill = billingMapper.selectById(billId);
        if (bill == null || !"PAID".equals(bill.getStatus())) {
            throw new BizException("不可退费");
        }
        bill.setStatus("REFUNDED");
        bill.setUpdatedAt(LocalDateTime.now());
        billingMapper.updateById(bill);

        FinancialTransaction tx = new FinancialTransaction();
        tx.setBizType("REFUND");
        tx.setDirection("OUT");
        tx.setAmount(bill.getAmount());
        tx.setStatus("DONE");
        tx.setRemark(reason);
        tx.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);

        auditService.log("REFUND", "财务退费 bill=" + billId);
        return tx;
    }

    @Transactional(rollbackFor = Exception.class)
    public FinancialTransaction reimbursement(BigDecimal amount, String reason) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setBizType("REIMBURSEMENT");
        tx.setDirection("OUT");
        tx.setAmount(amount);
        tx.setStatus("APPROVED");
        tx.setRemark(reason);
        tx.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);
        auditService.log("REIMBURSEMENT", "人员报销 amount=" + amount);
        return tx;
    }

    public String dailyReconcile() {
        QueryWrapper<FinancialTransaction> query = new QueryWrapper<>();
        // 获取今天的记录
        query.apply("DATE(created_at) = '" + LocalDate.now().toString() + "'");

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;

        for (FinancialTransaction t : transactionMapper.selectList(query)) {
            if ("IN".equals(t.getDirection())) income = income.add(t.getAmount());
            if ("OUT".equals(t.getDirection())) out = out.add(t.getAmount());
        }

        auditService.log("DAILY_RECONCILE", "日结对账");
        return "日结完成: 收入=" + income + " 支出=" + out + " 结余=" + income.subtract(out);
    }

    public PageResponse<Map<String, Object>> report(String date, long page, long size) {
        Page<FinancialTransaction> pageParam = new Page<>(PageSupport.page(page), PageSupport.size(size));
        QueryWrapper<FinancialTransaction> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(date)) {
            query.likeRight("created_at", date);
        }
        query.orderByDesc("created_at");
        transactionMapper.selectPage(pageParam, query);
        List<Map<String, Object>> records = pageParam.getRecords().stream()
                .map(this::transactionView)
                .toList();
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), records);
    }

    public PageResponse<Map<String, Object>> bills(String status, String keyword, String patientKeyword, long page, long size) {
        Page<BillingRecord> pageParam = new Page<>(PageSupport.page(page), PageSupport.size(size));
        QueryWrapper<BillingRecord> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            query.eq("status", status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like("id", keyword));
        }
        if (StringUtils.isNotBlank(patientKeyword)) {
            List<Long> patientIds = searchPatientIds(patientKeyword.trim());
            if (patientIds.isEmpty()) {
                return new PageResponse<>(PageSupport.page(page), PageSupport.size(size), 0, List.of());
            }
            query.in("patient_id", patientIds);
        }
        query.orderByDesc("created_at");
        billingMapper.selectPage(pageParam, query);
        Set<Long> patientIds = pageParam.getRecords().stream()
                .map(BillingRecord::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Patient> patientMap = patientIds.isEmpty()
                ? Map.of()
                : patientMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity(), (left, right) -> left));
        List<Map<String, Object>> records = pageParam.getRecords().stream()
                .map(bill -> billView(bill, patientMap.get(bill.getPatientId())))
                .toList();
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), records);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPrescriptionPaid(Long prescriptionId) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p != null) {
            p.setPaid("Y");
            p.setUpdatedAt(LocalDateTime.now());
            prescriptionMapper.updateById(p);
        }
    }

    private List<Long> searchPatientIds(String keyword) {
        QueryWrapper<Patient> query = new QueryWrapper<>();
        query.and(wrapper -> wrapper.like("name", keyword)
                .or().like("patient_no", keyword)
                .or().like("id_card", keyword));
        if (keyword.matches("\\d+")) {
            query.or().eq("id", Long.valueOf(keyword));
        }
        return patientMapper.selectList(query).stream()
                .map(Patient::getId)
                .toList();
    }

    private Map<String, Object> billView(BillingRecord bill, Patient patient) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", bill.getId());
        item.put("patientId", bill.getPatientId());
        item.put("patientName", patient == null ? "" : patient.getName());
        item.put("patientNo", patient == null ? "" : patient.getPatientNo());
        item.put("billingType", bill.getBillingType());
        item.put("amount", bill.getAmount());
        item.put("payChannel", bill.getPayChannel());
        item.put("settlementType", bill.getSettlementType());
        item.put("status", bill.getStatus());
        item.put("createdAt", bill.getCreatedAt());
        item.put("updatedAt", bill.getUpdatedAt());
        return item;
    }

    private Map<String, Object> transactionView(FinancialTransaction tx) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", tx.getId());
        item.put("bizType", tx.getBizType());
        item.put("amount", tx.getAmount());
        item.put("direction", tx.getDirection());
        item.put("status", tx.getStatus());
        item.put("remark", readablePatientDetail(tx.getRemark()));
        item.put("createdAt", tx.getCreatedAt());
        item.put("updatedAt", tx.getUpdatedAt());
        return item;
    }

    private String readablePatientDetail(String detail) {
        if (StringUtils.isBlank(detail)) {
            return detail;
        }
        Matcher matcher = PATIENT_ID_PATTERN.matcher(detail);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Patient patient = patientMapper.selectById(Long.valueOf(matcher.group(2)));
            if (patient == null) {
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + patientLabel(patient)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String patientLabel(Patient patient) {
        String patientNo = StringUtils.defaultIfBlank(patient.getPatientNo(), "ID " + patient.getId());
        return patient.getName() + "（" + patientNo + "）";
    }
}
