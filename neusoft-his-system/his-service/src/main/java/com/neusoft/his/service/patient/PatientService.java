package com.neusoft.his.service.patient;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.api.PageSupport;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorLeaveApplication;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Patient;
import com.neusoft.his.dal.mapper.DoctorLeaveApplicationMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * 患者管理业务服务。
 *
 * <p>负责患者建档、档案查询、挂号、缴费、退号和就诊状态维护。
 * 该层集中处理业务状态流转，并在关键操作后写入审计日志。</p>
 */
@Service
public class PatientService {
    private static final List<String> ACTIVE_DOCTOR_STATUSES = List.of("在岗", "在诊");
    private static final List<String> BLOCKING_LEAVE_STATUSES = List.of("待生效", "生效中");

    private final PatientMapper patientMapper;
    private final OutpatientRegistrationMapper registrationMapper;
    private final DoctorProfileMapper doctorMapper;
    private final DoctorScheduleMapper scheduleMapper;
    private final DoctorLeaveApplicationMapper leaveMapper;
    private final AuditService auditService;

    public PatientService(PatientMapper patientMapper, OutpatientRegistrationMapper registrationMapper,
                          DoctorProfileMapper doctorMapper, DoctorScheduleMapper scheduleMapper,
                          DoctorLeaveApplicationMapper leaveMapper, AuditService auditService) {
        this.patientMapper = patientMapper;
        this.registrationMapper = registrationMapper;
        this.doctorMapper = doctorMapper;
        this.scheduleMapper = scheduleMapper;
        this.leaveMapper = leaveMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Patient create(Patient patient) {
        // 1. 校验必填项 (数据库规范要求 name 不能为空)
        if (StringUtils.isBlank(patient.getName())) {
            throw new BizException("患者姓名不能为空");
        }

        // 2. 按身份证号查重：已存在则更新，不存在则新建
        if (StringUtils.isNotBlank(patient.getIdCard())) {
            Patient existing = patientMapper.selectByIdCard(patient.getIdCard());
            if (existing != null) {
                patient.setId(existing.getId());
                patient.setUpdatedAt(LocalDateTime.now());
                if (StringUtils.isBlank(patient.getPatientNo())) {
                    patient.setPatientNo(existing.getPatientNo());
                }
                if (StringUtils.isBlank(patient.getCurrentStatus())) {
                    patient.setCurrentStatus(existing.getCurrentStatus());
                }
                if (patient.getBalance() == null) {
                    patient.setBalance(existing.getBalance());
                }
                patientMapper.updateById(patient);
                auditService.log("PATIENT_UPDATE", "患者更新(按身份证号): " + patient.getName() + ", 身份证: " + patient.getIdCard());
                return patient;
            }
        }

        // 3. 自动生成唯一的患者编号 (如果前端未传入)
        if (StringUtils.isBlank(patient.getPatientNo())) {
            patient.setPatientNo(generatePatientNo());
        }

        // 4. 处理默认值，保证与数据库设计一致
        if (StringUtils.isBlank(patient.getGender())) {
            patient.setGender("未知");
        }
        if (StringUtils.isBlank(patient.getCurrentStatus())) {
            patient.setCurrentStatus("正常");
        }
        if (patient.getBalance() == null) {
            patient.setBalance(BigDecimal.ZERO);
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
        if (StringUtils.isBlank(patient.getPatientNo())) {
            patient.setPatientNo(old.getPatientNo());
        }
        if (StringUtils.isBlank(patient.getCurrentStatus())) {
            patient.setCurrentStatus(old.getCurrentStatus());
        }
        if (patient.getBalance() == null) {
            patient.setBalance(old.getBalance());
        }
        patient.setCreatedAt(old.getCreatedAt());
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
        auditService.log("PATIENT_UPDATE", "更新患者: " + id);
        return patient;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Patient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        patientMapper.deleteById(id);
        auditService.log("PATIENT_DELETE", "删除患者: " + id + ", 姓名: " + patient.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public Patient recharge(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("充值金额必须大于0");
        }
        Patient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        BigDecimal currentBalance = patient.getBalance() == null ? BigDecimal.ZERO : patient.getBalance();
        patient.setBalance(currentBalance.add(amount));
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
        auditService.log("PATIENT_RECHARGE", "患者充值: " + id + ", 金额: " + amount);
        return patient;
    }

    @Transactional(rollbackFor = Exception.class)
    public Patient refundBalance(Long id, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("退费金额必须大于0");
        }
        Patient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        BigDecimal currentBalance = patient.getBalance() == null ? BigDecimal.ZERO : patient.getBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new BizException("就诊卡余额不足");
        }
        patient.setBalance(currentBalance.subtract(amount));
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
        auditService.log("PATIENT_BALANCE_REFUND", "患者余额退费: " + id + ", 金额: " + amount + ", 原因: " + StringUtils.defaultIfBlank(reason, "未填写"));
        return patient;
    }

    public PageResponse<Patient> query(String keyword, long page, long size) {
        Page<Patient> pageParam = new Page<>(PageSupport.page(page), PageSupport.size(size));
        QueryWrapper<Patient> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            query.like("name", keyword).or().like("patient_no", keyword).or().like("id_card", keyword);
        }
        patientMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public OutpatientRegistration register(PatientRegistrationRequest req) {
        if (req == null || req.patientId() == null) {
            throw new BizException("请选择患者");
        }
        if (req.doctorId() == null) {
            throw new BizException("请选择看诊医生");
        }
        if (StringUtils.isBlank(req.scheduleDate())) {
            throw new BizException("请选择看诊日期");
        }
        Patient patient = patientMapper.selectById(req.patientId());
        if (patient == null) throw new BizException("患者不存在2");
        DoctorProfile doctor = doctorMapper.selectById(req.doctorId());
        if (doctor == null) {
            throw new BizException("医生不存在");
        }
        if (!ACTIVE_DOCTOR_STATUSES.contains(doctor.getAttendanceStatus())) {
            throw new BizException("医生当前处于" + StringUtils.defaultIfBlank(doctor.getAttendanceStatus(), "不可出诊") + "状态，不能挂号");
        }
        QueryWrapper<DoctorSchedule> scheduleQuery = new QueryWrapper<DoctorSchedule>()
                .eq("doctor_id", req.doctorId())
                .eq("schedule_date", req.scheduleDate())
                .eq("status", 1);
        if (StringUtils.isNotBlank(req.shift())) {
            scheduleQuery.eq("shift", req.shift());
        }
        List<DoctorSchedule> schedules = scheduleMapper.selectList(scheduleQuery);
        if (schedules == null || schedules.isEmpty()) {
            throw new BizException("该医生当前没有可挂号排班");
        }
        if (hasLeaveConflict(req.doctorId(), req.scheduleDate(), req.shift())) {
            throw new BizException("该医生在所选时间段已有请假记录，不能挂号");
        }
        long usedCount = registrationMapper.selectCount(new QueryWrapper<OutpatientRegistration>()
                .eq("doctor_id", req.doctorId())
                .eq("schedule_date", req.scheduleDate())
                .and(wrapper -> wrapper.isNull("status").or().notIn("status", "已退号", "已取消", "CANCELLED")));
        int limit = schedules.stream()
                .map(DoctorSchedule::getRegistrationLimit)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (limit <= 0 || usedCount >= limit) {
            throw new BizException("该医生号源已满，不能挂号");
        }

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

    private boolean hasLeaveConflict(Long doctorId, String scheduleDate, String shift) {
        LocalDate date = LocalDate.parse(scheduleDate);
        LocalDateTime slotStart = date.atTime(shiftStart(shift));
        LocalDateTime slotEnd = date.atTime(shiftEnd(shift));
        return leaveMapper.selectCount(new QueryWrapper<DoctorLeaveApplication>()
                .eq("doctor_id", doctorId)
                .in("status", BLOCKING_LEAVE_STATUSES)
                .lt("start_time", slotEnd)
                .gt("end_time", slotStart)) > 0;
    }

    private LocalTime shiftStart(String shift) {
        if ("下午".equals(shift)) {
            return LocalTime.of(13, 0);
        }
        return LocalTime.of(8, 0);
    }

    private LocalTime shiftEnd(String shift) {
        if ("上午".equals(shift)) {
            return LocalTime.of(12, 0);
        }
        if ("下午".equals(shift)) {
            return LocalTime.of(17, 0);
        }
        return LocalTime.of(17, 0);
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
    public Patient payRegistration(Long regId) {
        OutpatientRegistration reg = registrationMapper.selectById(regId);
        if (reg == null) throw new BizException("挂号单不存在");
        if ("Y".equals(reg.getPaid())) throw new BizException("该挂号单已缴费");
        Patient patient = patientMapper.selectById(reg.getPatientId());
        if (patient == null) {
            throw new BizException("患者不存在");
        }
        BigDecimal fee = reg.getFee() == null ? BigDecimal.ZERO : reg.getFee();
        BigDecimal currentBalance = patient.getBalance() == null ? BigDecimal.ZERO : patient.getBalance();
        if (currentBalance.compareTo(fee) < 0) {
            throw new BizException("就诊卡余额不足，请先充值");
        }

        reg.setPaid("Y");
        // 缴费完成后，业务状态流转至“待诊”
        reg.setStatus("待诊");
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(reg);
        patient.setBalance(currentBalance.subtract(fee));
        patient.setCurrentStatus("待诊");
        patient.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(patient);
        auditService.log("REG_PAY", "挂号缴费: " + regId + ", 扣费: " + fee);
        return patient;
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
        return registrations(id, status, null, page, size);
    }

    public PageResponse<Map<String, Object>> doctorRegistrations(Long userId, String status, long page, long size) {
        DoctorProfile doctor = doctorMapper.selectOne(new QueryWrapper<DoctorProfile>().eq("user_id", userId));
        if (doctor == null) {
            throw new BizException("医生档案不存在");
        }
        return registrations(null, status, doctor.getId(), page, size);
    }

    public PageResponse<Map<String, Object>> registrations(Long id, String status, Long doctorId, long page, long size) {
        Page<OutpatientRegistration> pageParam = new Page<>(PageSupport.page(page), PageSupport.size(size));
        QueryWrapper<OutpatientRegistration> query = new QueryWrapper<>();
        if (id != null) {
            query.eq("id", id);
        }
        if (StringUtils.isNotBlank(status)) {
            query.eq("status", status);
        }
        if (doctorId != null) {
            query.eq("doctor_id", doctorId);
        }
        query.orderByDesc("created_at");
        registrationMapper.selectPage(pageParam, query);
        // 只批量加载当前页涉及的患者和医生，避免列表每行触发一次数据库查询。
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
