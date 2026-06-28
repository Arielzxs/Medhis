package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditLogEntry;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequireRoles({RoleCode.ADMIN})
@Tag(name = "审计日志", description = "系统关键操作审计记录查询和清理")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    @Operation(summary = "分页查询审计日志", description = "管理员查看登录、挂号、收费、药房等关键操作日志。")
    public ApiResponse<PageResponse<AuditLogEntry>> logs(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                         @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(auditService.page(page, size));
    }

    @DeleteMapping("/logs")
    @Operation(summary = "清空审计日志", description = "管理员清空当前审计日志存储。")
    public ApiResponse<Void> clearLogs() {
        auditService.clear();
        return ApiResponse.ok("日志已清空", null);
    }
}
