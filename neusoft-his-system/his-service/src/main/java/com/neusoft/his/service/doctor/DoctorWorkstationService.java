package com.neusoft.his.service.doctor;

import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.Prescription;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DoctorWorkstationService {
    private final Map<Long, DoctorProfile> doctors = new ConcurrentHashMap<>();
    private final Map<Long, MedicalRecord> records = new ConcurrentHashMap<>();
    private final Map<Long, Prescription> prescriptions = new ConcurrentHashMap<>();
    private final AtomicLong doctorId = new AtomicLong(1);
    private final AtomicLong recordId = new AtomicLong(1);
    private final AtomicLong prescriptionId = new AtomicLong(1);
    private final AuditService auditService;

    public DoctorWorkstationService(AuditService auditService) {
        this.auditService = auditService;
    }

    public DoctorProfile saveDoctor(DoctorProfile doctor) {
        if (doctor.getId() == null) {
            doctor.setId(doctorId.getAndIncrement());
            doctor.setCreatedAt(LocalDateTime.now());
        } else {
            doctor.setUpdatedAt(LocalDateTime.now());
        }
        doctors.put(doctor.getId(), doctor);
        auditService.log("DOCTOR_SAVE", "医生档案: " + doctor.getName());
        return doctor;
    }

    public PageResponse<DoctorProfile> scheduleQuery(String department, long page, long size) {
        List<DoctorProfile> all = doctors.values().stream()
                .filter(d -> department == null || department.equals(d.getDepartment()))
                .sorted(Comparator.comparing(DoctorProfile::getId))
                .collect(Collectors.toList());
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return new PageResponse<>(page, size, all.size(), all.subList(from, to));
    }

    public String callPatient(Long patientId) {
        auditService.log("CALL_PATIENT", "呼叫患者: " + patientId);
        return "请患者" + patientId + "到诊室就诊";
    }

    public MedicalRecord saveRecord(MedicalRecord record) {
        if (record.getId() == null) {
            record.setId(recordId.getAndIncrement());
            record.setCreatedAt(LocalDateTime.now());
            record.setArchiveFlag("Y");
        }
        records.put(record.getId(), record);
        auditService.log("MEDICAL_RECORD", "病历=" + record.getId());
        return record;
    }

    public Prescription prescribe(Prescription prescription) {
        if (prescription.getPatientId() == null || prescription.getDoctorId() == null) {
            throw new BizException("处方患者和医生不能为空");
        }
        prescription.setId(prescriptionId.getAndIncrement());
        prescription.setAuditStatus("PENDING");
        prescription.setPaid("N");
        prescription.setDispenseStatus("WAITING");
        prescription.setCreatedAt(LocalDateTime.now());
        prescriptions.put(prescription.getId(), prescription);
        auditService.log("PRESCRIBE", "处方=" + prescription.getId());
        return prescription;
    }

    public List<MedicalRecord> history(Long patientId) {
        return records.values().stream().filter(r -> patientId.equals(r.getPatientId())).collect(Collectors.toList());
    }

    public Prescription getPrescription(Long id) {
        Prescription prescription = prescriptions.get(id);
        if (prescription == null) {
            throw new BizException("处方不存在");
        }
        return prescription;
    }

    public List<Prescription> listPrescriptions() {
        return records.isEmpty() ? prescriptions.values().stream().toList() : prescriptions.values().stream().toList();
    }
}
