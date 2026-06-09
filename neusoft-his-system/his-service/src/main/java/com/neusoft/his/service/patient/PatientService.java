package com.neusoft.his.service.patient;

import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class PatientService {
    private final Map<Long, Patient> patients = new ConcurrentHashMap<>();
    private final Map<Long, OutpatientRegistration> registrations = new ConcurrentHashMap<>();
    private final AtomicLong patientId = new AtomicLong(1);
    private final AtomicLong regId = new AtomicLong(1);
    private final AuditService auditService;

    public PatientService(AuditService auditService) {
        this.auditService = auditService;
    }

    public Patient create(Patient patient) {
        patient.setId(patientId.getAndIncrement());
        patient.setCurrentStatus("建档");
        patient.setCreatedAt(LocalDateTime.now());
        patients.put(patient.getId(), patient);
        auditService.log("PATIENT_CREATE", "患者建档: " + patient.getName());
        return patient;
    }

    public Patient update(Long id, Patient patient) {
        Patient old = patients.get(id);
        if (old == null) {
            throw new BizException("患者不存在");
        }
        patient.setId(id);
        patient.setCurrentStatus(old.getCurrentStatus());
        patient.setUpdatedAt(LocalDateTime.now());
        patients.put(id, patient);
        auditService.log("PATIENT_UPDATE", "更新患者: " + id);
        return patient;
    }

    public PageResponse<Patient> query(String keyword, long page, long size) {
        List<Patient> all = patients.values().stream()
                .filter(p -> keyword == null || p.getName().contains(keyword) || p.getPatientNo().contains(keyword))
                .sorted(Comparator.comparing(Patient::getId))
                .collect(Collectors.toList());
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return new PageResponse<>(page, size, all.size(), all.subList(from, to));
    }

    public OutpatientRegistration register(PatientRegistrationRequest req) {
        Patient patient = patients.get(req.patientId());
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        OutpatientRegistration reg = new OutpatientRegistration();
        reg.setId(regId.getAndIncrement());
        reg.setPatientId(req.patientId());
        reg.setDoctorId(req.doctorId());
        reg.setDepartment(req.department());
        reg.setScheduleDate(req.scheduleDate());
        reg.setFee(req.fee());
        reg.setPaid("N");
        reg.setStatus("已挂号");
        reg.setCreatedAt(LocalDateTime.now());
        registrations.put(reg.getId(), reg);
        patient.setCurrentStatus("待就诊");
        auditService.log("REGISTRATION", "挂号单=" + reg.getId());
        return reg;
    }

    public void cancelRegistration(Long regId) {
        OutpatientRegistration reg = registrations.get(regId);
        if (reg == null) {
            throw new BizException("挂号单不存在");
        }
        reg.setStatus("已退号");
        auditService.log("REG_CANCEL", "退号: " + regId);
    }

    public void payRegistration(Long regId) {
        OutpatientRegistration reg = registrations.get(regId);
        if (reg == null) {
            throw new BizException("挂号单不存在");
        }
        reg.setPaid("Y");
        auditService.log("REG_PAY", "挂号缴费: " + regId);
    }

    public String printRegistration(Long regId) {
        OutpatientRegistration reg = registrations.get(regId);
        if (reg == null) {
            throw new BizException("挂号单不存在");
        }
        return "挂号单#" + reg.getId() + " 患者=" + reg.getPatientId() + " 科室=" + reg.getDepartment() + " 日期=" + reg.getScheduleDate();
    }

    public String currentStatus(Long patientId) {
        Patient patient = patients.get(patientId);
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        return patient.getCurrentStatus();
    }
}
