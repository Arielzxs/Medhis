package com.neusoft.his.service.patient;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PatientMapper;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.service.dto.PatientRegistrationRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PatientService {
    private final PatientMapper patientMapper;
    private final OutpatientRegistrationMapper registrationMapper;
    private final DoctorProfileMapper doctorMapper;
    private final AuditService auditService;

    public PatientService(PatientMapper patientMapper, OutpatientRegistrationMapper registrationMapper,
                          DoctorProfileMapper doctorMapper, AuditService auditService) {
        this.patientMapper = patientMapper;
        this.registrationMapper = registrationMapper;
        this.doctorMapper = doctorMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Patient create(Patient patient) {
        // 1. 校验必填项 (数据库规范要求 name 不能为空)
        if (StringUtils.isBlank(patient.getName())) {
            throw new BizException("患者姓名不能为空");
        }

        // 2. 自动生成唯一的患者编号 (如果前端未传入)
        if (StringUtils.isBlank(patient.getPatientNo())) {
            patient.setPatientNo(generatePatientNo());
        }

        // 3. 处理默认值，保证与数据库设计一致
        if (StringUtils.isBlank(patient.getGender())) {
            patient.setGender("未知");
        }
        if (StringUtils.isBlank(patient.getCurrentStatus())) {
            patient.setCurrentStatus("正常");
        }

        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());

        patientMapper.insert(patient);
        auditService.log("PATIENT_CREATE", "患者建档: " + patient.getName() + ", 编号: " + patient.getPatientNo());
        return patient;
    }

    @Transactional(rollbackFor = Exception.class)
    public Patient update(Long id, Patient patient) {
        Patient old = patientMapper.selectById(id);
        if (old == null) throw new BizException("患者不存在");

        patient.setId(id);
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
        auditService.log("PATIENT_UPDATE", "更新患者: " + id);
        return patient;
    }

    public PageResponse<Patient> query(String keyword, long page, long size) {
        Page<Patient> pageParam = new Page<>(page, size);
        QueryWrapper<Patient> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            query.like("name", keyword).or().like("patient_no", keyword).or().like("id_card", keyword);
        }
        patientMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public OutpatientRegistration register(PatientRegistrationRequest req) {
        Patient patient = patientMapper.selectById(req.patientId());
        if (patient == null) throw new BizException("患者不存在");

        OutpatientRegistration reg = new OutpatientRegistration();
        reg.setPatientId(req.patientId());
        reg.setDoctorId(req.doctorId());
        reg.setDepartment(req.department());
        reg.setScheduleDate(req.scheduleDate());
        reg.setFee(req.fee());
        reg.setPaid("N");

        // 修改为符合数据库设计说明书中的初始状态 "待缴费"
        reg.setStatus("待缴费");

        reg.setCreatedAt(LocalDateTime.now());
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.insert(reg);

        // 同步更新患者当前的就诊状态
        patient.setCurrentStatus("待就诊");
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);

        auditService.log("REGISTRATION", "挂号单=" + reg.getId());
        return reg;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long regId) {
        OutpatientRegistration reg = registrationMapper.selectById(regId);
        if (reg == null) throw new BizException("挂号单不存在");

        // 已退号符合数据库设计说明书中的状态枚举
        reg.setStatus("已退号");
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(reg);
        auditService.log("REG_CANCEL", "退号: " + regId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void payRegistration(Long regId) {
        OutpatientRegistration reg = registrationMapper.selectById(regId);
        if (reg == null) throw new BizException("挂号单不存在");
        if ("Y".equals(reg.getPaid())) throw new BizException("该挂号单已缴费");

        reg.setPaid("Y");
        // 缴费完成后，业务状态流转至“待诊”
        reg.setStatus("待诊");
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(reg);
        auditService.log("REG_PAY", "挂号缴费: " + regId);
    }

    public String printRegistration(Long regId) {
        OutpatientRegistration reg = registrationMapper.selectById(regId);
        if (reg == null) throw new BizException("挂号单不存在");
        return "挂号单#" + reg.getId() + " 患者=" + reg.getPatientId() + " 科室=" + reg.getDepartment() + " 日期=" + reg.getScheduleDate();
    }

    public String currentStatus(Long patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) throw new BizException("患者不存在");
        return patient.getCurrentStatus();
    }

    public PageResponse<Map<String, Object>> registrations(Long id, String status, long page, long size) {
        Page<OutpatientRegistration> pageParam = new Page<>(page, size);
        QueryWrapper<OutpatientRegistration> query = new QueryWrapper<>();
        if (id != null) {
            query.eq("id", id);
        }
        if (StringUtils.isNotBlank(status)) {
            query.eq("status", status);
        }
        query.orderByDesc("created_at");
        registrationMapper.selectPage(pageParam, query);
        Map<Long, Patient> patientMap = batchPatients(pageParam.getRecords());
        Map<Long, DoctorProfile> doctorMap = batchDoctors(pageParam.getRecords());

        return new PageResponse<>(
                pageParam.getCurrent(),
                pageParam.getSize(),
                pageParam.getTotal(),
                pageParam.getRecords().stream().map(reg -> toRegistrationView(reg, patientMap, doctorMap)).toList()
        );
    }

    private Map<Long, Patient> batchPatients(List<OutpatientRegistration> records) {
        List<Long> patientIds = records.stream()
                .map(OutpatientRegistration::getPatientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (patientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return patientMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
    }

    private Map<Long, DoctorProfile> batchDoctors(List<OutpatientRegistration> records) {
        List<Long> doctorIds = records.stream()
                .map(OutpatientRegistration::getDoctorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (doctorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return doctorMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(DoctorProfile::getId, Function.identity()));
    }

    private Map<String, Object> toRegistrationView(OutpatientRegistration reg,
                                                   Map<Long, Patient> patientMap,
                                                   Map<Long, DoctorProfile> doctorMap) {
        Patient patient = patientMap.get(reg.getPatientId());
        DoctorProfile doctor = reg.getDoctorId() == null ? null : doctorMap.get(reg.getDoctorId());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", reg.getId());
        item.put("regNo", "REG" + reg.getId());
        item.put("patientId", reg.getPatientId());
        item.put("patientName", patient == null ? "" : patient.getName());
        item.put("doctorId", reg.getDoctorId());
        item.put("doctorName", doctor == null ? "" : doctor.getName());
        item.put("department", reg.getDepartment());
        item.put("scheduleDate", reg.getScheduleDate());
        item.put("status", reg.getStatus());
        item.put("fee", reg.getFee());
        item.put("paid", reg.getPaid());
        item.put("createdAt", reg.getCreatedAt());
        return item;
    }

    /**
     * 生成医疗机构患者唯一编号 (例：PT20260621XXXX)
     */
    private String generatePatientNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PT" + dateStr + randomStr;
    }
}
