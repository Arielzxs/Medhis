package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import com.neusoft.his.service.patient.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "患者管理", description = "患者档案、门诊挂号、缴费、退号和就诊状态查询")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "创建患者档案", description = "挂号员录入患者基础信息，生成患者档案。")
    public ApiResponse<Patient> create(@RequestBody Patient patient) {
        return ApiResponse.ok("建档成功", patientService.create(patient));
    }

    @PutMapping("/{id}")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "更新患者档案", description = "根据患者 ID 更新姓名、证件号、联系方式等基础资料。")
    public ApiResponse<Patient> update(@Parameter(description = "患者 ID") @PathVariable Long id,
                                       @RequestBody Patient patient) {
        return ApiResponse.ok("更新成功", patientService.update(id, patient));
    }

    @GetMapping
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.ADMIN})
    @Operation(summary = "分页查询患者档案", description = "按姓名、患者编号、身份证号或手机号关键字检索患者。")
    public ApiResponse<PageResponse<Patient>> query(@Parameter(description = "患者关键字") @RequestParam(required = false) String keyword,
                                                    @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(patientService.query(keyword, page, size));
    }

    @PostMapping("/registrations")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "办理门诊挂号", description = "根据患者、医生、科室、排班日期和挂号费生成挂号记录。")
    public ApiResponse<OutpatientRegistration> register(@RequestBody PatientRegistrationRequest req) {
        return ApiResponse.ok("挂号成功", patientService.register(req));
    }

    @GetMapping("/registrations")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.FINANCE, RoleCode.ADMIN})
    @Operation(summary = "分页查询挂号记录", description = "用于就诊状态追踪、医生候诊列表和财务收费场景。")
    public ApiResponse<PageResponse<java.util.Map<String, Object>>> registrations(@Parameter(description = "挂号单 ID") @RequestParam(required = false) Long id,
                                                                                  @Parameter(description = "挂号状态") @RequestParam(required = false) String status,
                                                                                  @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(patientService.registrations(id, status, page, size));
    }

    @PostMapping("/registrations/{id}/cancel")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "退号", description = "将指定挂号单状态更新为已取消。")
    public ApiResponse<Void> cancel(@Parameter(description = "挂号单 ID") @PathVariable Long id) {
        patientService.cancelRegistration(id);
        return ApiResponse.ok("退号成功", null);
    }

    @PostMapping("/registrations/{id}/pay")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.FINANCE, RoleCode.ADMIN})
    @Operation(summary = "挂号缴费", description = "将指定挂号单标记为已支付。")
    public ApiResponse<Void> pay(@Parameter(description = "挂号单 ID") @PathVariable Long id) {
        patientService.payRegistration(id);
        return ApiResponse.ok("挂号缴费成功", null);
    }

    @GetMapping("/registrations/{id}/print")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "打印挂号凭证", description = "返回挂号凭证文本内容，供前端打印。")
    public ApiResponse<String> print(@Parameter(description = "挂号单 ID") @PathVariable Long id) {
        return ApiResponse.ok(patientService.printRegistration(id));
    }

    @GetMapping("/{id}/status")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.ADMIN})
    @Operation(summary = "查询患者当前状态", description = "返回患者当前就诊状态，如待诊、就诊中、已完成等。")
    public ApiResponse<String> status(@Parameter(description = "患者 ID") @PathVariable Long id) {
        return ApiResponse.ok(patientService.currentStatus(id));
    }
}
