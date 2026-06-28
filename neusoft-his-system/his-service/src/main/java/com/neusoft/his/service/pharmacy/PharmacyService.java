package com.neusoft.his.service.pharmacy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.entity.Supplier;
import com.neusoft.his.dal.mapper.DrugCatalogMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import com.neusoft.his.dal.mapper.SupplierMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药房管理业务服务。
 *
 * <p>负责药品目录、供应商、库存入库/盘点、处方审核和发药。
 * 发药时会校验库存并扣减数量，保证库存数据与处方状态同步。</p>
 */
@Service
public class PharmacyService {
    private final DrugCatalogMapper drugMapper;
    private final SupplierMapper supplierMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final AuditService auditService;

    public PharmacyService(DrugCatalogMapper drugMapper, SupplierMapper supplierMapper,
                           PrescriptionMapper prescriptionMapper, AuditService auditService) {
        this.drugMapper = drugMapper;
        this.supplierMapper = supplierMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public DrugCatalog saveDrug(DrugCatalog drug) {
        validateDrug(drug);
        drug.setCode(drug.getCode().trim());
        drug.setName(drug.getName().trim());
        if (drug.getUnit() != null) {
            drug.setUnit(drug.getUnit().trim());
        }

        QueryWrapper<DrugCatalog> duplicate = new QueryWrapper<DrugCatalog>().eq("code", drug.getCode());
        if (drug.getId() != null) {
            duplicate.ne("id", drug.getId());
        }
        if (drugMapper.selectCount(duplicate) > 0) {
            throw new BizException("药品编码已存在");
        }
        if (drug.getStock() == null) {
            drug.setStock(0);
        }
        if (drug.getWarningThreshold() == null) {
            drug.setWarningThreshold(0);
        }
        if (drug.getId() == null) {
            drug.setCreatedAt(LocalDateTime.now());
            drugMapper.insert(drug);
        } else {
            drug.setUpdatedAt(LocalDateTime.now());
            drugMapper.updateById(drug);
        }
        auditService.log("DRUG_SAVE", "药品: " + drug.getCode() + " " + drug.getName());
        return drug;
    }

    private void validateDrug(DrugCatalog drug) {
        if (StringUtils.isBlank(drug.getCode())) {
            throw new BizException("药品编码不能为空");
        }
        if (StringUtils.isBlank(drug.getName())) {
            throw new BizException("药品名称不能为空");
        }
        if (StringUtils.isBlank(drug.getUnit())) {
            throw new BizException("药品单位不能为空");
        }
        if (drug.getPrice() == null || drug.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("药品价格不能小于0");
        }
        if (drug.getStock() != null && drug.getStock() < 0) {
            throw new BizException("药品库存不能小于0");
        }
        if (drug.getWarningThreshold() != null && drug.getWarningThreshold() < 0) {
            throw new BizException("库存预警线不能小于0");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            supplier.setCreatedAt(LocalDateTime.now());
            supplierMapper.insert(supplier);
        } else {
            supplier.setUpdatedAt(LocalDateTime.now());
            supplierMapper.updateById(supplier);
        }
        return supplier;
    }

    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long drugId, int quantity) {
        DrugCatalog drug = drugMapper.selectById(drugId);
        if (drug == null) throw new BizException("药品不存在");
        drug.setStock((drug.getStock() == null ? 0 : drug.getStock()) + quantity);
        drug.setUpdatedAt(LocalDateTime.now());
        drugMapper.updateById(drug);
        auditService.log("DRUG_INBOUND", "入库 drug=" + drugId + " qty=" + quantity);
    }

    public PageResponse<DrugCatalog> inventory(String codeKeyword, String nameKeyword, Boolean warningOnly, long page, long size) {
        Page<DrugCatalog> pageParam = new Page<>(page, size);
        QueryWrapper<DrugCatalog> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(codeKeyword)) {
            query.like("code", codeKeyword.trim());
        }
        if (StringUtils.isNotBlank(nameKeyword)) {
            query.like("name", nameKeyword.trim());
        }
        if (Boolean.TRUE.equals(warningOnly)) {
            query.apply("stock <= warning_threshold");
        }
        query.orderByAsc("code").orderByDesc("updated_at");
        drugMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    public List<DrugCatalog> stockWarnings() {
        QueryWrapper<DrugCatalog> query = new QueryWrapper<>();
        query.apply("stock < warning_threshold");
        return drugMapper.selectList(query);
    }

    @Transactional(rollbackFor = Exception.class)
    public void stocktake(Long drugId, int realStock) {
        DrugCatalog drug = drugMapper.selectById(drugId);
        if (drug == null) throw new BizException("药品不存在");
        drug.setStock(realStock);
        drug.setUpdatedAt(LocalDateTime.now());
        drugMapper.updateById(drug);
        auditService.log("STOCKTAKE", "盘点 drug=" + drugId + " realStock=" + realStock);
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription reviewPrescription(Long prescriptionId, boolean approved) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) throw new BizException("处方不存在");
        p.setAuditStatus(approved ? "APPROVED" : "REJECTED");
        p.setUpdatedAt(LocalDateTime.now());
        prescriptionMapper.updateById(p);
        return p;
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription dispense(Long prescriptionId, Long drugId, int quantity) {
        Prescription p = prescriptionMapper.selectById(prescriptionId);
        if (p == null) throw new BizException("处方不存在");
        if (!"APPROVED".equals(p.getAuditStatus()) || !"Y".equals(p.getPaid())) {
            throw new BizException("处方未审核通过或未缴费");
        }

        // 核心修复：基于数据库层面的原子性库存扣减，防止高并发超卖
        UpdateWrapper<DrugCatalog> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", drugId)
                .ge("stock", quantity) // 校验现有库存必须大于等于扣减量
                .setSql("stock = stock - " + quantity);

        boolean success = drugMapper.update(null, updateWrapper) > 0;
        if (!success) {
            throw new BizException("药品库存不足或操作冲突");
        }

        p.setDispenseStatus("DONE");
        prescriptionMapper.updateById(p);

        auditService.log("DISPENSE", "发药 rx=" + prescriptionId + " drug=" + drugId + " qty=" + quantity);
        return p;
    }
}
