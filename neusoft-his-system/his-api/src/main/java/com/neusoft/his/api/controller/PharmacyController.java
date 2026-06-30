package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.entity.Supplier;
import com.neusoft.his.service.pharmacy.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
@Tag(name = "药房管理", description = "药品目录、供应商、库存入库盘点、处方审核和发药")
public class PharmacyController {
    private final PharmacyService service;

    public PharmacyController(PharmacyService service) {
        this.service = service;
    }

    @PostMapping("/drugs")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "保存药品", description = "新增或更新药品目录、价格、库存和预警阈值。")
    public ApiResponse<DrugCatalog> saveDrug(@RequestBody DrugCatalog drug) {
        return ApiResponse.ok(service.saveDrug(drug));
    }

    @PostMapping("/suppliers")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "保存供应商", description = "新增或更新药品供应商档案。")
    public ApiResponse<Supplier> saveSupplier(@RequestBody Supplier supplier) {
        return ApiResponse.ok(service.saveSupplier(supplier));
    }

    @PostMapping("/drugs/{id}/inbound")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "药品入库", description = "增加指定药品库存数量。")
    public ApiResponse<Void> inbound(@Parameter(description = "药品 ID") @PathVariable Long id,
                                     @Parameter(description = "入库数量") @RequestParam int quantity) {
        service.inbound(id, quantity);
        return ApiResponse.ok("入库成功", null);
    }

    @GetMapping("/inventory")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "分页查询库存", description = "按药品编码、名称和库存预警状态筛选药品库存。")
    public ApiResponse<PageResponse<DrugCatalog>> inventory(@Parameter(description = "药品编码关键字") @RequestParam(required = false) String codeKeyword,
                                                            @Parameter(description = "药品名称关键字") @RequestParam(required = false) String nameKeyword,
                                                            @Parameter(description = "药品分类") @RequestParam(required = false) String category,
                                                            @Parameter(description = "是否只看库存预警") @RequestParam(required = false) Boolean warningOnly,
                                                            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.inventory(codeKeyword, nameKeyword, category, warningOnly, page, size));
    }

    @GetMapping("/inventory/warnings")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "查询库存预警", description = "返回库存低于预警阈值的药品列表。")
    public ApiResponse<List<DrugCatalog>> warnings() {
        return ApiResponse.ok(service.stockWarnings());
    }

    @PostMapping("/inventory/{id}/stocktake")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "库存盘点", description = "按实盘数量校准指定药品库存。")
    public ApiResponse<Void> stocktake(@Parameter(description = "药品 ID") @PathVariable Long id,
                                       @Parameter(description = "实盘库存") @RequestParam int realStock) {
        service.stocktake(id, realStock);
        return ApiResponse.ok("盘点完成", null);
    }

    @PostMapping("/prescriptions/{id}/review")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "处方审核", description = "药房审核医生处方是否通过。")
    public ApiResponse<Prescription> review(@Parameter(description = "处方 ID") @PathVariable Long id,
                                            @Parameter(description = "是否审核通过") @RequestParam boolean approved) {
        return ApiResponse.ok(service.reviewPrescription(id, approved));
    }

    @GetMapping("/prescriptions/{id}/payment-info")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "查询处方缴费信息", description = "查询处方金额、患者余额和是否足够缴费。")
    public ApiResponse<Map<String, Object>> paymentInfo(@Parameter(description = "处方 ID") @PathVariable Long id) {
        return ApiResponse.ok(service.prescriptionPaymentInfo(id));
    }

    @PostMapping("/prescriptions/{id}/pay")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "处方缴费", description = "从患者就诊卡余额扣除处方金额，并将处方标记为已缴费。")
    public ApiResponse<Map<String, Object>> pay(@Parameter(description = "处方 ID") @PathVariable Long id) {
        return ApiResponse.ok("缴费成功", service.payPrescription(id));
    }

    @PostMapping("/patients/{id}/recharge")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "患者充值", description = "在药房发药前为患者就诊卡充值。")
    public ApiResponse<Map<String, Object>> recharge(@Parameter(description = "患者 ID") @PathVariable Long id,
                                                     @Parameter(description = "充值金额") @RequestParam BigDecimal amount) {
        return ApiResponse.ok("充值成功", service.rechargePatient(id, amount));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
    @Operation(summary = "处方发药", description = "按处方扣减指定药品库存并更新发药状态。")
    public ApiResponse<Prescription> dispense(@Parameter(description = "处方 ID") @PathVariable Long id,
                                              @Parameter(description = "药品 ID") @RequestParam Long drugId,
                                              @Parameter(description = "发药数量") @RequestParam int quantity) {
        return ApiResponse.ok(service.dispense(id, drugId, quantity));
    }
}
