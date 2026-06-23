package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.service.analytics.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequireRoles({RoleCode.ADMIN})
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/outpatient-trend")
    public ApiResponse<Map<String, Long>> outpatientTrend() {
        return ApiResponse.ok(service.outpatientTrend());
    }

    @GetMapping("/doctor-workload")
    public ApiResponse<Map<String, Long>> doctorWorkload() {
        return ApiResponse.ok(service.doctorWorkload());
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }
}
