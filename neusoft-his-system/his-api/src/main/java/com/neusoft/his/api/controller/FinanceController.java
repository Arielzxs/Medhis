package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.BillingRecord;
import com.neusoft.his.dal.entity.FinancialTransaction;
import com.neusoft.his.service.finance.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/finance")
@RequireRoles({RoleCode.FINANCE, RoleCode.ADMIN})
@Tag(name = "财务管理", description = "处方计价、收费、退费、报销、支出和日结报表")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @PostMapping("/prescriptions/{id}/price")
    @Operation(summary = "处方计价", description = "根据处方内容生成待收费账单。")
    public ApiResponse<BillingRecord> price(@Parameter(description = "处方 ID") @PathVariable Long id) {
        return ApiResponse.ok(service.pricePrescription(id));
    }

    @PostMapping("/bills/{id}/charge")
    @Operation(summary = "账单收费", description = "对待收费账单完成收费并记录支付渠道。")
    public ApiResponse<BillingRecord> charge(@Parameter(description = "账单 ID") @PathVariable Long id,
                                             @Parameter(description = "支付渠道，如 CASH、WECHAT、ALIPAY") @RequestParam String channel,
                                             @Parameter(description = "结算类型，默认自费") @RequestParam(defaultValue = "SELF_PAY") String settlementType) {
        return ApiResponse.ok(service.charge(id, channel, settlementType));
    }

    @GetMapping("/bills")
    @Operation(summary = "分页查询账单", description = "按状态、账单关键字或患者关键字查询收费记录。")
    public ApiResponse<PageResponse<BillingRecord>> bills(@Parameter(description = "账单状态") @RequestParam(required = false) String status,
                                                          @Parameter(description = "账单关键字") @RequestParam(required = false) String keyword,
                                                          @Parameter(description = "患者关键字") @RequestParam(required = false) String patientKeyword,
                                                          @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                          @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.bills(status, keyword, patientKeyword, page, size));
    }

    @PostMapping("/prescriptions/{id}/mark-paid")
    @Operation(summary = "标记处方已缴费", description = "收费完成后同步处方缴费状态。")
    public ApiResponse<Void> markPaid(@Parameter(description = "处方 ID") @PathVariable Long id) {
        service.markPrescriptionPaid(id);
        return ApiResponse.ok("处方已标记缴费", null);
    }

    @PostMapping("/expense")
    @Operation(summary = "登记支出", description = "登记医院运营支出流水。")
    public ApiResponse<FinancialTransaction> expense(@Parameter(description = "支出金额") @RequestParam BigDecimal amount,
                                                     @Parameter(description = "支出说明") @RequestParam String remark) {
        return ApiResponse.ok(service.expense(amount, remark));
    }

    @PostMapping("/bills/{id}/refund")
    @Operation(summary = "账单退费", description = "对已收费账单发起退费并记录原因。")
    public ApiResponse<FinancialTransaction> refund(@Parameter(description = "账单 ID") @PathVariable Long id,
                                                    @Parameter(description = "退费原因") @RequestParam String reason) {
        return ApiResponse.ok(service.refund(id, reason));
    }

    @PostMapping("/reimbursement")
    @Operation(summary = "登记报销", description = "登记报销类财务流水。")
    public ApiResponse<FinancialTransaction> reimbursement(@Parameter(description = "报销金额") @RequestParam BigDecimal amount,
                                                           @Parameter(description = "报销原因") @RequestParam String reason) {
        return ApiResponse.ok(service.reimbursement(amount, reason));
    }

    @PostMapping("/reconcile/daily")
    @Operation(summary = "执行日结", description = "汇总当日收入、支出和净收入，返回日结结果。")
    public ApiResponse<String> daily() {
        return ApiResponse.ok(service.dailyReconcile());
    }

    @GetMapping("/reports")
    @Operation(summary = "分页查询财务报表", description = "按日期查询财务流水和报表数据。")
    public ApiResponse<PageResponse<FinancialTransaction>> reports(@Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam(required = false) String date,
                                                                   @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                   @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.report(date, page, size));
    }
}
