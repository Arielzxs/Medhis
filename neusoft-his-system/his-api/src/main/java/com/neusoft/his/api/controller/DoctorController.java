package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.service.dto.DoctorScheduleView;
import com.neusoft.his.service.doctor.DoctorWorkstationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorWorkstationService service;

    public DoctorController(DoctorWorkstationService service) {
        this.service = service;
    }

    @GetMapping
    @RequireRoles({RoleCode.DOCTOR, RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<List<DoctorProfile>> doctors(@RequestParam(required = false) String department) {
        return ApiResponse.ok(service.listDoctors(department));
    }

    @PostMapping
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<DoctorProfile> saveDoctor(@RequestBody DoctorProfile doctor) {
        return ApiResponse.ok(service.saveDoctor(doctor));
    }

    @GetMapping("/schedules")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<PageResponse<DoctorScheduleView>> schedules(@RequestParam(required = false) String department,
                                                                   @RequestParam(required = false) String doctorName,
                                                                   @RequestParam(required = false) String date,
                                                                   @RequestParam(defaultValue = "1") long page,
                                                                   @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.scheduleQuery(department, doctorName, date, page, size));
    }

    @PostMapping("/schedules")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    public ApiResponse<DoctorSchedule> saveSchedule(@RequestBody DoctorSchedule schedule) {
        return ApiResponse.ok("排班保存成功", service.saveSchedule(schedule));
    }

    @PutMapping("/schedules/{id}")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    public ApiResponse<DoctorSchedule> updateSchedule(@PathVariable Long id, @RequestBody DoctorSchedule schedule) {
        schedule.setId(id);
        return ApiResponse.ok("排班更新成功", service.saveSchedule(schedule));
    }

    @DeleteMapping("/schedules/{id}")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.ADMIN})
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        service.deleteSchedule(id);
        return ApiResponse.ok("排班已删除", null);
    }

    @PostMapping("/call/{patientId}")
    @RequireRoles({RoleCode.DOCTOR})
    public ApiResponse<String> call(@PathVariable Long patientId) {
        return ApiResponse.ok(service.callPatient(patientId));
    }

    @PostMapping("/records")
    @RequireRoles({RoleCode.DOCTOR})
    public ApiResponse<MedicalRecord> saveRecord(@RequestBody MedicalRecord record) {
        return ApiResponse.ok("病历已归档", service.saveRecord(record));
    }

    @GetMapping("/records/history/{patientId}")
    @RequireRoles({RoleCode.DOCTOR})
    public ApiResponse<List<MedicalRecord>> history(@PathVariable Long patientId) {
        return ApiResponse.ok(service.history(patientId));
    }

    @PostMapping("/prescriptions")
    @RequireRoles({RoleCode.DOCTOR})
    public ApiResponse<Prescription> prescribe(@RequestBody Prescription prescription) {
        return ApiResponse.ok("处方开具成功", service.prescribe(prescription));
    }
    @GetMapping("/prescriptions")
    public ApiResponse<List<Prescription>> getAllPrescriptions() {
        return ApiResponse.ok(service.listPrescriptions());
    }
}
