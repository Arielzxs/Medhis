package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.entity.Supplier;
import com.neusoft.his.service.pharmacy.PharmacyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
@RequireRoles({RoleCode.PHARMACY_ADMIN, RoleCode.ADMIN})
public class PharmacyController {
    private final PharmacyService service;

    public PharmacyController(PharmacyService service) {
        this.service = service;
    }

    @PostMapping("/drugs")
    public ApiResponse<DrugCatalog> saveDrug(@RequestBody DrugCatalog drug) {
        return ApiResponse.ok(service.saveDrug(drug));
    }

    @PostMapping("/suppliers")
    public ApiResponse<Supplier> saveSupplier(@RequestBody Supplier supplier) {
        return ApiResponse.ok(service.saveSupplier(supplier));
    }

    @PostMapping("/drugs/{id}/inbound")
    public ApiResponse<Void> inbound(@PathVariable Long id, @RequestParam int quantity) {
        service.inbound(id, quantity);
        return ApiResponse.ok("入库成功", null);
    }

    @GetMapping("/inventory")
    public ApiResponse<PageResponse<DrugCatalog>> inventory(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.inventory(page, size));
    }

    @GetMapping("/inventory/warnings")
    public ApiResponse<List<DrugCatalog>> warnings() {
        return ApiResponse.ok(service.stockWarnings());
    }

    @PostMapping("/inventory/{id}/stocktake")
    public ApiResponse<Void> stocktake(@PathVariable Long id, @RequestParam int realStock) {
        service.stocktake(id, realStock);
        return ApiResponse.ok("盘点完成", null);
    }

    @PostMapping("/prescriptions/{id}/review")
    public ApiResponse<Prescription> review(@PathVariable Long id, @RequestParam boolean approved) {
        return ApiResponse.ok(service.reviewPrescription(id, approved));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    public ApiResponse<Prescription> dispense(@PathVariable Long id, @RequestParam Long drugId, @RequestParam int quantity) {
        return ApiResponse.ok(service.dispense(id, drugId, quantity));
    }
}
