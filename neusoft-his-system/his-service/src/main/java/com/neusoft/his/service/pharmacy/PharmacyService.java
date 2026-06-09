package com.neusoft.his.service.pharmacy;

import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.entity.Supplier;
import com.neusoft.his.service.doctor.DoctorWorkstationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class PharmacyService {
    private final Map<Long, DrugCatalog> drugs = new ConcurrentHashMap<>();
    private final Map<Long, Supplier> suppliers = new ConcurrentHashMap<>();
    private final AtomicLong drugId = new AtomicLong(1);
    private final AtomicLong supplierId = new AtomicLong(1);
    private final AuditService auditService;
    private final DoctorWorkstationService workstationService;

    public PharmacyService(AuditService auditService, DoctorWorkstationService workstationService) {
        this.auditService = auditService;
        this.workstationService = workstationService;
    }

    public DrugCatalog saveDrug(DrugCatalog drug) {
        if (drug.getId() == null) {
            drug.setId(drugId.getAndIncrement());
            drug.setCreatedAt(LocalDateTime.now());
            if (drug.getStock() == null) {
                drug.setStock(0);
            }
        }
        drugs.put(drug.getId(), drug);
        auditService.log("DRUG_SAVE", "药品维护: " + drug.getName());
        return drug;
    }

    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            supplier.setId(supplierId.getAndIncrement());
            supplier.setCreatedAt(LocalDateTime.now());
        }
        suppliers.put(supplier.getId(), supplier);
        auditService.log("SUPPLIER_SAVE", "供应商维护: " + supplier.getName());
        return supplier;
    }

    public void inbound(Long drugId, int quantity) {
        DrugCatalog drug = drugs.get(drugId);
        if (drug == null) {
            throw new BizException("药品不存在");
        }
        drug.setStock(drug.getStock() + quantity);
        auditService.log("DRUG_INBOUND", "入库 drug=" + drugId + " qty=" + quantity);
    }

    public List<DrugCatalog> stockWarnings() {
        return drugs.values().stream()
                .filter(d -> d.getStock() <= d.getWarningThreshold())
                .sorted(Comparator.comparing(DrugCatalog::getStock))
                .collect(Collectors.toList());
    }

    public PageResponse<DrugCatalog> inventory(long page, long size) {
        List<DrugCatalog> all = drugs.values().stream().sorted(Comparator.comparing(DrugCatalog::getId)).toList();
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return new PageResponse<>(page, size, all.size(), all.subList(from, to));
    }

    public void stocktake(Long drugId, int realStock) {
        DrugCatalog drug = drugs.get(drugId);
        if (drug == null) {
            throw new BizException("药品不存在");
        }
        drug.setStock(realStock);
        auditService.log("STOCKTAKE", "盘点 drug=" + drugId + " realStock=" + realStock);
    }

    public Prescription reviewPrescription(Long prescriptionId, boolean approved) {
        Prescription p = workstationService.getPrescription(prescriptionId);
        p.setAuditStatus(approved ? "APPROVED" : "REJECTED");
        auditService.log("RX_REVIEW", "处方审核 id=" + prescriptionId + " result=" + p.getAuditStatus());
        return p;
    }

    public Prescription dispense(Long prescriptionId, Long drugId, int quantity) {
        Prescription p = workstationService.getPrescription(prescriptionId);
        if (!"APPROVED".equals(p.getAuditStatus()) || !"Y".equals(p.getPaid())) {
            throw new BizException("处方未审核通过或未缴费");
        }
        DrugCatalog drug = drugs.get(drugId);
        if (drug == null || drug.getStock() < quantity) {
            throw new BizException("库存不足");
        }
        drug.setStock(drug.getStock() - quantity);
        p.setDispenseStatus("DONE");
        auditService.log("DISPENSE", "发药 rx=" + prescriptionId + " drug=" + drugId + " qty=" + quantity);
        return p;
    }
}
