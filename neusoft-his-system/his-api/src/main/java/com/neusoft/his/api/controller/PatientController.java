package com.neusoft.his.api.controller;

import com.neusoft.his.common.api.ApiResponse;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.security.RequireRoles;
import com.neusoft.his.common.security.RoleCode;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import com.neusoft.his.service.patient.PatientService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<Patient> create(@RequestBody Patient patient) {
        return ApiResponse.ok("建档成功", patientService.create(patient));
    }

    @PutMapping("/{id}")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<Patient> update(@PathVariable Long id, @RequestBody Patient patient) {
        return ApiResponse.ok("更新成功", patientService.update(id, patient));
    }

    @GetMapping
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.ADMIN})
    public ApiResponse<PageResponse<Patient>> query(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(patientService.query(keyword, page, size));
    }

    @PostMapping("/registrations")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<OutpatientRegistration> register(@RequestBody PatientRegistrationRequest req) {
        return ApiResponse.ok("挂号成功", patientService.register(req));
    }

    @PostMapping("/registrations/{id}/cancel")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        patientService.cancelRegistration(id);
        return ApiResponse.ok("退号成功", null);
    }

    @PostMapping("/registrations/{id}/pay")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.FINANCE, RoleCode.ADMIN})
    public ApiResponse<Void> pay(@PathVariable Long id) {
        patientService.payRegistration(id);
        return ApiResponse.ok("挂号缴费成功", null);
    }

    @GetMapping("/registrations/{id}/print")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.ADMIN})
    public ApiResponse<String> print(@PathVariable Long id) {
        return ApiResponse.ok(patientService.printRegistration(id));
    }

    @GetMapping("/{id}/status")
    @RequireRoles({RoleCode.REGISTRAR, RoleCode.DOCTOR, RoleCode.ADMIN})
    public ApiResponse<String> status(@PathVariable Long id) {
        return ApiResponse.ok(patientService.currentStatus(id));
    }
}
