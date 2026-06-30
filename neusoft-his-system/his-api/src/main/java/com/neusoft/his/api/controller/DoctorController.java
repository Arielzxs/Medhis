package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.common.security.SecurityUser;
import com.neusoft.his.common.security.SecurityUserHolder;
import com.neusoft.his.dal.entity.DoctorLeaveApplication;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.DrugCatalog;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.view.DoctorScheduleView;
import com.neusoft.his.service.doctor.DoctorWorkstationService;
import com.neusoft.his.service.dto.DoctorLeaveApplyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@Tag(name = "医生工作站", description = "医生档案、排班、叫号、病历和处方管理")
public class DoctorController {
    private final DoctorWorkstationService service;

    public DoctorController(DoctorWorkstationService service) {
        this.service = service;
    }

    @GetMapping
    @RequireRoles({RoleCode.DOCTOR, RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "查询医生列表", description = "按科室筛选医生档案，用于排班和挂号选择。")
    public ApiResponse<List<DoctorProfile>> doctors(@Parameter(description = "科室名称") @RequestParam(required = false) String department) {
        return ApiResponse.ok(service.listDoctors(department));
    }

    @PostMapping
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "保存医生档案", description = "新增或更新医生基础档案。")
    public ApiResponse<DoctorProfile> saveDoctor(@RequestBody DoctorProfile doctor) {
        return ApiResponse.ok(service.saveDoctor(doctor));
    }

    @GetMapping("/profiles")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询医生档案", description = "管理员按科室、在岗状态和关键字维护医生业务档案。")
    public ApiResponse<List<DoctorProfile>> profiles(@Parameter(description = "科室名称") @RequestParam(required = false) String department,
                                                      @Parameter(description = "在岗状态") @RequestParam(required = false) String attendanceStatus,
                                                      @Parameter(description = "姓名或专长关键字") @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.listDoctorProfiles(department, attendanceStatus, keyword));
    }

    @GetMapping("/leaves")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "查询医生请假记录", description = "管理员按医生、状态和时间段筛选请假记录。")
    public ApiResponse<List<DoctorLeaveApplication>> leaves(@Parameter(description = "医生档案 ID") @RequestParam(required = false) Long doctorId,
                                                            @Parameter(description = "请假状态") @RequestParam(required = false) String status,
                                                            @Parameter(description = "筛选开始时间") @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                            @Parameter(description = "筛选结束时间") @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.ok(service.listLeaves(doctorId, status, startTime, endTime));
    }

    @PostMapping("/leaves")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "新增医生请假记录", description = "管理员为指定医生新增请假时间段。")
    public ApiResponse<DoctorLeaveApplication> saveLeave(@RequestBody DoctorLeaveApplication leave) {
        return ApiResponse.ok("请假记录已保存", service.saveLeaveByAdmin(leave));
    }

    @PutMapping("/leaves/{id}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "修改医生请假记录", description = "管理员修改指定医生请假时间段和原因。")
    public ApiResponse<DoctorLeaveApplication> updateLeave(@Parameter(description = "请假记录 ID") @PathVariable Long id,
                                                           @RequestBody DoctorLeaveApplication leave) {
        leave.setId(id);
        return ApiResponse.ok("请假记录已更新", service.saveLeaveByAdmin(leave));
    }

    @PostMapping("/leaves/{id}/cancel")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "撤销医生请假记录", description = "管理员撤销待生效或生效中的医生请假记录。")
    public ApiResponse<DoctorLeaveApplication> cancelLeave(@Parameter(description = "请假记录 ID") @PathVariable Long id) {
        return ApiResponse.ok("请假已撤销", service.cancelLeaveByAdmin(id));
    }

    @DeleteMapping("/leaves/{id}")
    @RequireRoles({RoleCode.ADMIN})
    @Operation(summary = "删除医生请假记录", description = "管理员删除指定医生请假记录。")
    public ApiResponse<Void> deleteLeave(@Parameter(description = "请假记录 ID") @PathVariable Long id) {
        service.deleteLeaveByAdmin(id);
        return ApiResponse.ok("请假记录已删除", null);
    }

    @GetMapping("/me/profile")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "查询我的医生档案", description = "医生查看自己的业务档案。")
    public ApiResponse<DoctorProfile> myProfile() {
        return ApiResponse.ok(service.getOwnDoctorProfile(currentUserId()));
    }

    @PutMapping("/me/profile")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "更新我的医生档案", description = "医生仅可维护自己的档案信息，不能修改其他医生档案。")
    public ApiResponse<DoctorProfile> updateMyProfile(@RequestBody DoctorProfile doctor) {
        return ApiResponse.ok("个人档案已更新", service.updateOwnDoctorProfile(currentUserId(), doctor));
    }

    @PostMapping("/me/leave")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "提交请假", description = "医生提交时间段请假申请，生效期间个人在岗状态自动变为休息。")
    public ApiResponse<DoctorLeaveApplication> submitLeave(@RequestBody DoctorLeaveApplyRequest request) {
        return ApiResponse.ok("请假申请已提交", service.submitLeave(currentUserId(), request));
    }

    @GetMapping("/me/leaves")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "查询我的请假记录", description = "医生查看自己提交过的请假时间段和当前生效状态。")
    public ApiResponse<List<DoctorLeaveApplication>> myLeaves() {
        return ApiResponse.ok(service.listOwnLeaves(currentUserId()));
    }

    @PostMapping("/me/leaves/{id}/cancel")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "撤销我的请假", description = "医生撤销自己的待生效或生效中请假申请。")
    public ApiResponse<DoctorLeaveApplication> cancelMyLeave(@Parameter(description = "请假记录 ID") @PathVariable Long id) {
        return ApiResponse.ok("请假已撤销", service.cancelOwnLeave(currentUserId(), id));
    }

    @PostMapping("/me/resume")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "恢复在诊", description = "医生结束休息后恢复在诊，原有有效排班自动重新开放挂号。")
    public ApiResponse<DoctorProfile> resumeWork() {
        return ApiResponse.ok("已恢复在诊", service.resumeWork(currentUserId()));
    }

    @GetMapping("/schedules")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.REGISTRAR, RoleCode.ADMIN})
    @Operation(summary = "分页查询医生排班", description = "门诊挂号页使用该接口查询号源、剩余号和排班信息。")
    public ApiResponse<PageResponse<DoctorScheduleView>> schedules(@Parameter(description = "科室名称") @RequestParam(required = false) String department,
                                                                   @Parameter(description = "医生姓名关键字") @RequestParam(required = false) String doctorName,
                                                                   @Parameter(description = "排班日期，格式 yyyy-MM-dd") @RequestParam(required = false) String date,
                                                                   @Parameter(description = "是否只查询可挂号号源") @RequestParam(defaultValue = "false") boolean availableOnly,
                                                                   @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                   @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.scheduleQuery(department, doctorName, date, availableOnly, page, size));
    }

    @PostMapping("/schedules")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    @Operation(summary = "新增排班", description = "保存医生某日某班次的出诊安排。")
    public ApiResponse<DoctorSchedule> saveSchedule(@RequestBody DoctorSchedule schedule) {
        return ApiResponse.ok("排班保存成功", service.saveSchedule(schedule));
    }

    @PutMapping("/schedules/{id}")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    @Operation(summary = "更新排班", description = "按排班 ID 更新出诊日期、班次、限额和状态。")
    public ApiResponse<DoctorSchedule> updateSchedule(@Parameter(description = "排班 ID") @PathVariable Long id,
                                                      @RequestBody DoctorSchedule schedule) {
        schedule.setId(id);
        return ApiResponse.ok("排班更新成功", service.saveSchedule(schedule));
    }

    @DeleteMapping("/schedules/{id}")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    @Operation(summary = "删除排班", description = "删除指定医生排班。")
    public ApiResponse<Void> deleteSchedule(@Parameter(description = "排班 ID") @PathVariable Long id) {
        service.deleteSchedule(id);
        return ApiResponse.ok("排班已删除", null);
    }

    @PostMapping("/call/{patientId}")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "叫号", description = "医生工作站呼叫指定患者就诊。")
    public ApiResponse<String> call(@Parameter(description = "患者 ID") @PathVariable Long patientId) {
        return ApiResponse.ok(service.callPatient(patientId));
    }

    @PostMapping("/records")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "保存病历", description = "医生保存诊断、处置计划并归档病历。")
    public ApiResponse<MedicalRecord> saveRecord(@RequestBody MedicalRecord record) {
        return ApiResponse.ok("病历已归档", service.saveRecordForDoctor(currentUserId(), record));
    }

    @GetMapping("/records/history/{patientId}")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "查询历史病历", description = "按患者 ID 查询历史诊疗记录。")
    public ApiResponse<List<MedicalRecord>> history(@Parameter(description = "患者 ID") @PathVariable Long patientId) {
        return ApiResponse.ok(service.history(patientId));
    }

    @GetMapping("/drugs")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "医生开方药品搜索", description = "医生在接诊时按药品编码或名称搜索可开具药品和库存。")
    public ApiResponse<PageResponse<DrugCatalog>> drugs(@Parameter(description = "药品编码关键字") @RequestParam(required = false) String codeKeyword,
                                                        @Parameter(description = "药品名称关键字") @RequestParam(required = false) String nameKeyword,
                                                        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                        @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.searchDrugsForPrescription(codeKeyword, nameKeyword, page, size));
    }

    @PostMapping("/prescriptions")
    @RequireRoles({RoleCode.DOCTOR})
    @Operation(summary = "开具处方", description = "医生为患者开具药品或检查处方。")
    public ApiResponse<Prescription> prescribe(@RequestBody Prescription prescription) {
        return ApiResponse.ok("处方开具成功", service.prescribeForDoctor(currentUserId(), prescription));
    }
    @GetMapping("/prescriptions")
    @Operation(summary = "分页查询处方", description = "分页返回处方数据，避免一次返回全部处方导致文档页或前端卡顿。")
    public ApiResponse<PageResponse<Map<String, Object>>> getAllPrescriptions(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.listPrescriptionViews(page, size));
    }

    private Long currentUserId() {
        SecurityUser user = SecurityUserHolder.get();
        if (user == null || user.userId() == null) {
            throw new BizException("请先登录");
        }
        return user.userId();
    }
}
