package com.neusoft.his.service.finance;

import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.BillingRecord;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.service.doctor.DoctorWorkstationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FinanceService {
    private final Map<Long, BillingRecord> bills = new ConcurrentHashMap<>();
    private final Map<Long, FinancialTransaction> transactions = new ConcurrentHashMap<>();
    private final AtomicLong billId = new AtomicLong(1);
    private final AtomicLong txId = new AtomicLong(1);
    private final AuditService auditService;
    private final DoctorWorkstationService workstationService;

    public FinanceService(AuditService auditService, DoctorWorkstationService workstationService) {
        this.auditService = auditService;
        this.workstationService = workstationService;
    }

    public BillingRecord pricePrescription(Long prescriptionId) {
        Prescription p = workstationService.getPrescription(prescriptionId);
        BillingRecord billing = new BillingRecord();
        billing.setId(billId.getAndIncrement());
        billing.setPatientId(p.getPatientId());
        billing.setBillingType("PRESCRIPTION");
        billing.setAmount(p.getTotalAmount());
        billing.setStatus("PRICED");
        billing.setCreatedAt(LocalDateTime.now());
        bills.put(billing.getId(), billing);
        auditService.log("PRICE", "处方划价 bill=" + billing.getId());
        return billing;
    }

    public BillingRecord charge(Long billId, String channel, String settlementType) {
        BillingRecord bill = bills.get(billId);
        if (bill == null) {
            throw new BizException("账单不存在");
        }
        bill.setPayChannel(channel);
        bill.setSettlementType(settlementType);
        bill.setStatus("PAID");

        FinancialTransaction tx = new FinancialTransaction();
        tx.setId(txId.getAndIncrement());
        tx.setBizType("OUTPATIENT_CHARGE");
        tx.setDirection("IN");
        tx.setAmount(bill.getAmount());
        tx.setStatus("DONE");
        tx.setCreatedAt(LocalDateTime.now());
        transactions.put(tx.getId(), tx);
        auditService.log("CHARGE", "门诊收费 bill=" + billId + " channel=" + channel);
        return bill;
    }

    public FinancialTransaction expense(BigDecimal amount, String remark) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setId(txId.getAndIncrement());
        tx.setBizType("PURCHASE_EXPENSE");
        tx.setDirection("OUT");
        tx.setAmount(amount);
        tx.setStatus("DONE");
        tx.setRemark(remark);
        tx.setCreatedAt(LocalDateTime.now());
        transactions.put(tx.getId(), tx);
        auditService.log("EXPENSE", "采购支出 amount=" + amount);
        return tx;
    }

    public FinancialTransaction refund(Long billId, String reason) {
        BillingRecord bill = bills.get(billId);
        if (bill == null || !"PAID".equals(bill.getStatus())) {
            throw new BizException("不可退费");
        }
        bill.setStatus("REFUNDED");
        FinancialTransaction tx = new FinancialTransaction();
        tx.setId(txId.getAndIncrement());
        tx.setBizType("REFUND");
        tx.setDirection("OUT");
        tx.setAmount(bill.getAmount());
        tx.setStatus("DONE");
        tx.setRemark(reason);
        tx.setCreatedAt(LocalDateTime.now());
        transactions.put(tx.getId(), tx);
        auditService.log("REFUND", "财务退费 bill=" + billId);
        return tx;
    }

    public FinancialTransaction reimbursement(BigDecimal amount, String reason) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setId(txId.getAndIncrement());
        tx.setBizType("REIMBURSEMENT");
        tx.setDirection("OUT");
        tx.setAmount(amount);
        tx.setStatus("APPROVED");
        tx.setRemark(reason);
        tx.setCreatedAt(LocalDateTime.now());
        transactions.put(tx.getId(), tx);
        auditService.log("REIMBURSEMENT", "人员报销 amount=" + amount);
        return tx;
    }

    public String dailyReconcile() {
        BigDecimal income = transactions.values().stream()
                .filter(t -> "IN".equals(t.getDirection()))
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal out = transactions.values().stream()
                .filter(t -> "OUT".equals(t.getDirection()))
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        auditService.log("DAILY_RECONCILE", "日结对账");
        return "日结完成: 收入=" + income + " 支出=" + out + " 结余=" + income.subtract(out);
    }

    public PageResponse<FinancialTransaction> report(long page, long size) {
        List<FinancialTransaction> all = transactions.values().stream()
                .sorted(Comparator.comparing(FinancialTransaction::getId))
                .toList();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return new PageResponse<>(page, size, all.size(), all.subList(from, to));
    }

    public void markPrescriptionPaid(Long prescriptionId) {
        Prescription p = workstationService.getPrescription(prescriptionId);
        p.setPaid("Y");
    }
}
