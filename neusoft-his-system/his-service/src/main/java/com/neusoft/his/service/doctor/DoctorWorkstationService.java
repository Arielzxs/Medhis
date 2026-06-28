package com.neusoft.his.service.doctor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import com.neusoft.his.dal.view.DoctorScheduleView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 医生工作站业务服务。
 *
 * <p>负责医生档案、排班号源、病历、处方和叫号等业务。排班查询面向挂号页，
 * 因此会特别控制分页和剩余号源统计的查询范围。</p>
 */
@Service
public class DoctorWorkstationService {
    private final DoctorProfileMapper doctorMapper;
    private final DoctorScheduleMapper scheduleMapper;
    private final MedicalRecordMapper recordMapper;
    private final OutpatientRegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final AuditService auditService;

    public DoctorWorkstationService(DoctorProfileMapper doctorMapper, DoctorScheduleMapper scheduleMapper,
                                    MedicalRecordMapper recordMapper, OutpatientRegistrationMapper registrationMapper,
                                    PrescriptionMapper prescriptionMapper, AuditService auditService) {
        this.doctorMapper = doctorMapper;
        this.scheduleMapper = scheduleMapper;
        this.recordMapper = recordMapper;
        this.registrationMapper = registrationMapper;
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

    public List<DoctorProfile> listDoctors(String department) {
        QueryWrapper<DoctorProfile> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(department)) {
            query.eq("department", department);
        }
        query.orderByAsc("department").orderByAsc("name");
        return doctorMapper.selectList(query);
    }

    public PageResponse<DoctorScheduleView> scheduleQuery(String department, String doctorName, String date,
                                                          long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        Page<Object> pageParam = new Page<>(safePage, safeSize);
        List<DoctorScheduleView> records = scheduleMapper.selectSchedulePage(pageParam, department, doctorName, date);
        long total = scheduleMapper.countSchedulePage(department, doctorName, date);
        // 剩余号源只基于当前页排班批量统计，避免对全部挂号记录做全表聚合。
        List<DoctorScheduleView> recordsWithRemain = fillScheduleRemain(records);
        return new PageResponse<>(safePage, safeSize, total, recordsWithRemain);
    }

    private List<DoctorScheduleView> fillScheduleRemain(List<DoctorScheduleView> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<String, DoctorScheduleMapper.ScheduleRegistrationCount> usedCountMap =
                scheduleMapper.countRegistrationsForSchedules(records).stream()
                        .collect(Collectors.toMap(
                                count -> scheduleKey(count.doctorId(), count.scheduleDate()),
                                Function.identity(),
                                (left, right) -> left
                        ));
        return records.stream()
                .map(record -> withRemain(record, usedCountMap))
                .collect(Collectors.toList());
    }

    private DoctorScheduleView withRemain(DoctorScheduleView record,
                                          Map<String, DoctorScheduleMapper.ScheduleRegistrationCount> usedCountMap) {
        long usedCount = usedCountMap.getOrDefault(
                scheduleKey(record.doctorId(), record.scheduleDate()),
                new DoctorScheduleMapper.ScheduleRegistrationCount(record.doctorId(), record.scheduleDate(), 0L)
        ).usedCount();
        int limit = record.limit() == null ? 0 : record.limit();
        int remain = record.status() != null && record.status() == 0
                ? 0
                : Math.max((int) (limit - Math.min(usedCount, limit)), 0);
        return new DoctorScheduleView(
                record.id(),
                record.doctorId(),
                record.scheduleDate(),
                record.shift(),
                record.department(),
                record.doctorName(),
                record.name(),
                record.title(),
                record.attendanceStatus(),
                record.level(),
                record.limit(),
                remain,
                record.status()
        );
    }

    private String scheduleKey(Long doctorId, String scheduleDate) {
        return doctorId + "#" + scheduleDate;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorSchedule saveSchedule(DoctorSchedule schedule) {
        validateSchedule(schedule);
        DoctorProfile doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null) {
            throw new BizException("医生不存在");
        }
        QueryWrapper<DoctorSchedule> duplicate = new QueryWrapper<DoctorSchedule>()
                .eq("doctor_id", schedule.getDoctorId())
                .eq("schedule_date", schedule.getScheduleDate())
                .eq("shift", schedule.getShift());
        if (schedule.getId() != null) {
            duplicate.ne("id", schedule.getId());
        }
        if (scheduleMapper.selectCount(duplicate) > 0) {
            throw new BizException("该医生当天该班次已存在排班");
        }
        if (schedule.getRegistrationLimit() == null) {
            schedule.setRegistrationLimit(50);
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);
        }
        if (schedule.getId() == null) {
            schedule.setCreatedAt(LocalDateTime.now());
            scheduleMapper.insert(schedule);
        } else {
            schedule.setUpdatedAt(LocalDateTime.now());
            scheduleMapper.updateById(schedule);
        }
        auditService.log("DOCTOR_SCHEDULE_SAVE", "排班: " + doctor.getName() + " " + schedule.getScheduleDate() + " " + schedule.getShift());
        return schedule;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        DoctorSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BizException("排班不存在");
        }
        scheduleMapper.deleteById(id);
        auditService.log("DOCTOR_SCHEDULE_DELETE", "删除排班: " + id);
    }

    private void validateSchedule(DoctorSchedule schedule) {
        if (schedule.getDoctorId() == null) {
            throw new BizException("请选择医生");
        }
        if (StringUtils.isBlank(schedule.getScheduleDate())) {
            throw new BizException("请选择排班日期");
        }
        if (StringUtils.isBlank(schedule.getShift())) {
            throw new BizException("请选择班次");
        }
        if (schedule.getRegistrationLimit() != null && schedule.getRegistrationLimit() < 0) {
            throw new BizException("挂号限额不能小于0");
        }
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
