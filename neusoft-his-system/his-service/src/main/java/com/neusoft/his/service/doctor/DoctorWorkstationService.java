package com.neusoft.his.service.doctor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorWorkstationService {
    private final DoctorProfileMapper doctorMapper;
    private final MedicalRecordMapper recordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final AuditService auditService;

    public DoctorWorkstationService(DoctorProfileMapper doctorMapper, MedicalRecordMapper recordMapper,
                                    PrescriptionMapper prescriptionMapper, AuditService auditService) {
        this.doctorMapper = doctorMapper;
        this.recordMapper = recordMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorProfile saveDoctor(DoctorProfile doctor) {
        if (doctor.getId() == null) {
            doctor.setCreatedAt(LocalDateTime.now());
            doctorMapper.insert(doctor);
        } else {
            doctor.setUpdatedAt(LocalDateTime.now());
            doctorMapper.updateById(doctor);
        }
        auditService.log("DOCTOR_SAVE", "医生档案: " + doctor.getName());
        return doctor;
    }

    public PageResponse<DoctorProfile> scheduleQuery(String department, long page, long size) {
        Page<DoctorProfile> pageParam = new Page<>(page, size);
        QueryWrapper<DoctorProfile> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(department)) {
            query.eq("department", department);
        }
        doctorMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    public String callPatient(Long patientId) {
        auditService.log("CALL_PATIENT", "呼叫患者: " + patientId);
        return "请患者" + patientId + "到诊室就诊";
    }

    @Transactional(rollbackFor = Exception.class)
    public MedicalRecord saveRecord(MedicalRecord record) {
        if (record.getId() == null) {
            record.setCreatedAt(LocalDateTime.now());
            record.setArchiveFlag("Y");
            recordMapper.insert(record);
        } else {
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);
        }
        auditService.log("MEDICAL_RECORD", "病历保存: patient=" + record.getPatientId());
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public Prescription prescribe(Prescription prescription) {
        if (prescription.getPatientId() == null || prescription.getDoctorId() == null) {
            throw new BizException("处方患者和医生不能为空");
        }
        prescription.setAuditStatus("PENDING");
        prescription.setPaid("N");
        prescription.setDispenseStatus("WAITING");
        prescription.setCreatedAt(LocalDateTime.now());
        prescriptionMapper.insert(prescription);
        auditService.log("PRESCRIBE", "处方开具=" + prescription.getId());
        return prescription;
    }

    public List<MedicalRecord> history(Long patientId) {
        QueryWrapper<MedicalRecord> query = new QueryWrapper<>();
        query.eq("patient_id", patientId).orderByDesc("created_at");
        return recordMapper.selectList(query);
    }

    public Prescription getPrescription(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) throw new BizException("处方不存在");
        return prescription;
    }

    public List<Prescription> listPrescriptions() {
        return prescriptionMapper.selectList(null);
    }
}