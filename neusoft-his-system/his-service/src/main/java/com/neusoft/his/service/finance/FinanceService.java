package com.neusoft.his.service.finance;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.BillingRecord;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.BillingRecordMapper;
import com.neusoft.his.dal.mapper.FinancialTransactionMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class FinanceService {
    private final BillingRecordMapper billingMapper;
    private final FinancialTransactionMapper transactionMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final AuditService auditService;

    public FinanceService(BillingRecordMapper billingMapper, FinancialTransactionMapper transactionMapper,
                          PrescriptionMapper prescriptionMapper, AuditService auditService) {
        this.billingMapper = billingMapper;
        this.transactionMapper = transactionMapper;
        this.prescriptionMapper = prescriptionMapper;
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

    public PageResponse<FinancialTransaction> report(String date, long page, long size) {
        Page<FinancialTransaction> pageParam = new Page<>(page, size);
        QueryWrapper<FinancialTransaction> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(date)) {
            query.likeRight("created_at", date);
        }
        query.orderByDesc("created_at");
        transactionMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    public PageResponse<BillingRecord> bills(String status, String keyword, String patientKeyword, long page, long size) {
        Page<BillingRecord> pageParam = new Page<>(page, size);
        QueryWrapper<BillingRecord> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            query.eq("status", status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like("id", keyword));
        }
        if (StringUtils.isNotBlank(patientKeyword)) {
            query.and(wrapper -> wrapper.like("patient_id", patientKeyword));
        }
        query.orderByDesc("created_at");
        billingMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
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
}
