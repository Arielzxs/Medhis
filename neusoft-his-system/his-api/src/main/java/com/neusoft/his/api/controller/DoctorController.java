package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.Prescription;
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

    @PostMapping
    @RequireRoles({RoleCode.ADMIN})
    public ApiResponse<DoctorProfile> saveDoctor(@RequestBody DoctorProfile doctor) {
        return ApiResponse.ok(service.saveDoctor(doctor));
    }

    @GetMapping("/schedules")
    @RequireRoles({RoleCode.DOCTOR, RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<PageResponse<DoctorProfile>> schedules(@RequestParam(required = false) String department,
                                                              @RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(service.scheduleQuery(department, page, size));
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
