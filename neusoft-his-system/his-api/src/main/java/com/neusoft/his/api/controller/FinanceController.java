package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.BillingRecord;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.service.finance.FinanceService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/finance")
@RequireRoles({RoleCode.FINANCE, RoleCode.ADMIN})
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @PostMapping("/prescriptions/{id}/price")
    public ApiResponse<BillingRecord> price(@PathVariable Long id) {
        return ApiResponse.ok(service.pricePrescription(id));
    }

    @PostMapping("/bills/{id}/charge")
    public ApiResponse<BillingRecord> charge(@PathVariable Long id,
                                             @RequestParam String channel,
                                             @RequestParam(defaultValue = "SELF_PAY") String settlementType) {
        return ApiResponse.ok(service.charge(id, channel, settlementType));
    }

    @GetMapping("/bills")
    public ApiResponse<PageResponse<BillingRecord>> bills(@RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.bills(status, page, size));
    }

    @PostMapping("/prescriptions/{id}/mark-paid")
    public ApiResponse<Void> markPaid(@PathVariable Long id) {
        service.markPrescriptionPaid(id);
        return ApiResponse.ok("处方已标记缴费", null);
    }

    @PostMapping("/expense")
    public ApiResponse<FinancialTransaction> expense(@RequestParam BigDecimal amount, @RequestParam String remark) {
        return ApiResponse.ok(service.expense(amount, remark));
    }

    @PostMapping("/bills/{id}/refund")
    public ApiResponse<FinancialTransaction> refund(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.ok(service.refund(id, reason));
    }

    @PostMapping("/reimbursement")
    public ApiResponse<FinancialTransaction> reimbursement(@RequestParam BigDecimal amount, @RequestParam String reason) {
        return ApiResponse.ok(service.reimbursement(amount, reason));
    }

    @PostMapping("/reconcile/daily")
    public ApiResponse<String> daily() {
        return ApiResponse.ok(service.dailyReconcile());
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<FinancialTransaction>> reports(@RequestParam(defaultValue = "1") long page,
                                                                   @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.report(page, size));
    }
}
