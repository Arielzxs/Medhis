package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.Department;
import com.neusoft.his.service.department.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "科室管理", description = "医院科室基础资料维护")
public class DepartmentController {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    @RequireRoles({RoleCode.ADMIN, RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.FINANCE, RoleCode.PHARMACY_ADMIN})
    @Operation(summary = "查询科室", description = "查询科室基础资料，可用于业务下拉选项。")
    public ApiResponse<List<Department>> list(@Parameter(description = "科室关键字") @RequestParam(required = false) String keyword,
                                               @Parameter(description = "是否只返回启用科室") @RequestParam(defaultValue = "false") boolean enabledOnly) {
        return ApiResponse.ok(service.list(keyword, enabledOnly));
    }

    @PostMapping
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "保存科室", description = "管理员新增或更新科室基础资料。")
    public ApiResponse<Department> save(@RequestBody Department department) {
        return ApiResponse.ok("科室已保存", service.save(department));
    }

    @PutMapping("/{id}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "更新科室", description = "管理员按 ID 更新科室基础资料。")
    public ApiResponse<Department> update(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        return ApiResponse.ok("科室已更新", service.save(department));
    }

    @PostMapping("/{id}/enabled")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "启停科室", description = "管理员启用或停用指定科室。")
    public ApiResponse<Department> updateEnabled(@PathVariable Long id,
                                                 @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        return ApiResponse.ok("科室状态已更新", service.updateEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "删除科室", description = "删除未被医生档案引用的科室。")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("科室已删除", null);
    }
}
