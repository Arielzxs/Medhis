package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.service.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequireRoles({RoleCode.ADMIN})
@Tag(name = "统计分析", description = "运营看板、门诊趋势和医生工作量统计")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/outpatient-trend")
    @Operation(summary = "门诊趋势统计", description = "统计门诊挂号趋势数据，用于运营分析图表。")
    public ApiResponse<Map<String, Long>> outpatientTrend() {
        return ApiResponse.ok(service.outpatientTrend());
    }

    @GetMapping("/doctor-workload")
    @RequireRoles({RoleCode.ADMIN, RoleCode.DOCTOR})
    @Operation(summary = "医生工作量统计", description = "统计医生接诊、病历等工作量指标。")
    public ApiResponse<Map<String, Long>> doctorWorkload() {
        return ApiResponse.ok(service.doctorWorkload());
    }

    @GetMapping("/doctor-workload/ranking")
    @RequireRoles({RoleCode.ADMIN, RoleCode.DOCTOR})
    @Operation(summary = "医生工作量排行", description = "按科室和日期范围查询医生接诊人次与处方数量排行。")
    public ApiResponse<List<Map<String, Object>>> doctorWorkloadRanking(@RequestParam(required = false) String department,
                                                                        @RequestParam(required = false) LocalDate startDate,
                                                                        @RequestParam(required = false) LocalDate endDate) {
        return ApiResponse.ok(service.doctorWorkloadRanking(department, startDate, endDate));
    }

    @GetMapping("/drug-consumption")
    @RequireRoles({RoleCode.ADMIN, RoleCode.PHARMACY_ADMIN})
    @Operation(summary = "药品消耗排行", description = "按药品分类和日期范围统计已发药处方中的药品消耗数量。")
    public ApiResponse<List<Map<String, Object>>> drugConsumption(@RequestParam(required = false) String category,
                                                                  @RequestParam(required = false) LocalDate startDate,
                                                                  @RequestParam(required = false) LocalDate endDate) {
        return ApiResponse.ok(service.drugConsumptionRanking(category, startDate, endDate));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "首页运营看板", description = "汇总今日挂号、候诊、收入和库存预警等首页指标。")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }
}
