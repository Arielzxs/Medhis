package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.common.security.SecurityUser;
import com.neusoft.his.common.security.SecurityUserHolder;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import com.neusoft.his.service.patient.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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

    @DeleteMapping("/{id}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "删除患者档案", description = "管理员删除指定患者基础档案。")
    public ApiResponse<Void> delete(@Parameter(description = "患者 ID") @PathVariable Long id) {
        patientService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/{id}/recharge")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.FINANCE, RoleCode.ADMIN})
    @Operation(summary = "就诊卡充值", description = "为已确认的患者档案增加就诊卡余额。")
    public ApiResponse<Patient> recharge(@Parameter(description = "患者 ID") @PathVariable Long id,
                                         @Parameter(description = "充值金额") @RequestParam BigDecimal amount) {
        return ApiResponse.ok("充值成功", patientService.recharge(id, amount));
    }

    @PostMapping("/{id}/balance/refund")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.FINANCE, RoleCode.ADMIN})
    @Operation(summary = "就诊卡余额退费", description = "从已确认患者的就诊卡余额中退回指定金额。")
    public ApiResponse<Patient> refundBalance(@Parameter(description = "患者 ID") @PathVariable Long id,
                                              @Parameter(description = "退费金额") @RequestParam BigDecimal amount,
                                              @Parameter(description = "退费原因") @RequestParam(required = false) String reason) {
        return ApiResponse.ok("退费成功", patientService.refundBalance(id, amount, reason));
    }


    //上方患者档案撞见内容












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
                                                                                  @Parameter(description = "医生档案 ID") @RequestParam(required = false) Long doctorId,
                                                                                  @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        SecurityUser user = SecurityUserHolder.get();
        if (user != null
                && user.roles() != null
                && user.roles().contains(RoleCode.DOCTOR)
                && !user.roles().contains(RoleCode.ADMIN)
                && !user.roles().contains(RoleCode.REGISTRAR)
                && !user.roles().contains(RoleCode.FINANCE)) {
            return ApiResponse.ok(patientService.doctorRegistrations(user.userId(), status, page, size));
        }
        return ApiResponse.ok(patientService.registrations(id, status, doctorId, page, size));
    }

    @GetMapping("/registrations/me")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "查询我的候诊队列", description = "医生仅查询分配给自己的挂号患者。")
    public ApiResponse<PageResponse<java.util.Map<String, Object>>> myRegistrations(@Parameter(description = "挂号状态") @RequestParam(required = false) String status,
                                                                                    @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(patientService.doctorRegistrations(currentUserId(), status, page, size));
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
    public ApiResponse<Patient> pay(@Parameter(description = "挂号单 ID") @PathVariable Long id) {
        return ApiResponse.ok("挂号缴费成功", patientService.payRegistration(id));
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

    @GetMapping("/idcard/parse")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "身份证号解析", description = "根据 18 位身份证号提取出生日期和性别。第 18 位校验码支持 X/x。")
    public ApiResponse<java.util.Map<String, Object>> parseIdCard(@Parameter(description = "18 位身份证号") @RequestParam String card) {
        if (card == null || card.length() != 18) {
            return ApiResponse.fail("身份证号必须为 18 位");
        }
        // 前 17 位必须全为数字，第 18 位可以是数字或 X/x
        if (!card.substring(0, 17).matches("\\d{17}")) {
            return ApiResponse.fail("身份证号前 17 位必须为数字");
        }
        char checkDigit = card.charAt(17);
        if (!Character.isDigit(checkDigit) && checkDigit != 'X' && checkDigit != 'x') {
            return ApiResponse.fail("身份证号第 18 位校验码格式不正确");
        }
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        // 第 7-14 位为出生日期 YYYYMMDD
        String birthStr = card.substring(6, 14);
        info.put("birthday", birthStr.substring(0, 4) + "-" + birthStr.substring(4, 6) + "-" + birthStr.substring(6, 8));
        // 第 17 位为性别标识（序列码最后一位），奇数为男，偶数为女。这一位永远是数字，不可能是 X
        int genderDigit = Integer.parseInt(card.substring(16, 17));
        info.put("gender", genderDigit % 2 == 1 ? "男" : "女");
        return ApiResponse.ok(info);
    }

    private Long currentUserId() {
        SecurityUser user = SecurityUserHolder.get();
        if (user == null || user.userId() == null) {
            throw new com.neusoft.his.common.exception.BizException("请先登录");
        }
        return user.userId();
    }
}
