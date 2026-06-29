package com.neusoft.his.service.doctor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.his.common.api.PageResponse;
import com.neusoft.his.common.api.PageSupport;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.DoctorLeaveApplication;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.entity.DoctorSchedule;
import com.neusoft.his.dal.entity.MedicalRecord;
import com.neusoft.his.dal.entity.OutpatientRegistration;
import com.neusoft.his.dal.entity.Prescription;
import com.neusoft.his.dal.mapper.DoctorLeaveApplicationMapper;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import com.neusoft.his.dal.mapper.DoctorScheduleMapper;
import com.neusoft.his.dal.mapper.MedicalRecordMapper;
import com.neusoft.his.dal.mapper.OutpatientRegistrationMapper;
import com.neusoft.his.dal.mapper.PrescriptionMapper;
import com.neusoft.his.dal.view.DoctorScheduleView;
import com.neusoft.his.service.dto.DoctorLeaveApplyRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final List<String> ACTIVE_ATTENDANCE_STATUSES = List.of("在岗", "在诊");
    private static final String REST_ATTENDANCE_STATUS = "休息";
    private static final String DEFAULT_WORK_STATUS = "在诊";
    private static final String LEAVE_STATUS_WAITING = "待生效";
    private static final String LEAVE_STATUS_ACTIVE = "生效中";
    private static final String LEAVE_STATUS_FINISHED = "已结束";
    private static final String LEAVE_STATUS_CANCELLED = "已撤销";

    private final DoctorProfileMapper doctorMapper;
    private final DoctorLeaveApplicationMapper leaveMapper;
    private final DoctorScheduleMapper scheduleMapper;
    private final MedicalRecordMapper recordMapper;
    private final OutpatientRegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final AuditService auditService;

    public DoctorWorkstationService(DoctorProfileMapper doctorMapper, DoctorLeaveApplicationMapper leaveMapper,
                                    DoctorScheduleMapper scheduleMapper,
                                    MedicalRecordMapper recordMapper, OutpatientRegistrationMapper registrationMapper,
                                    PrescriptionMapper prescriptionMapper, AuditService auditService) {
        this.doctorMapper = doctorMapper;
        this.leaveMapper = leaveMapper;
        this.scheduleMapper = scheduleMapper;
        this.recordMapper = recordMapper;
        this.registrationMapper = registrationMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorProfile saveDoctor(DoctorProfile doctor) {
        validateDoctorProfile(doctor);
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
            query.eq("department", department.trim());
        }
        query.in("attendance_status", ACTIVE_ATTENDANCE_STATUSES);
        query.orderByAsc("department").orderByAsc("name");
        return doctorMapper.selectList(query);
    }

    public List<DoctorProfile> listDoctorProfiles(String department, String attendanceStatus, String keyword) {
        QueryWrapper<DoctorProfile> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(department)) {
            query.eq("department", department.trim());
        }
        if (StringUtils.isNotBlank(attendanceStatus)) {
            query.eq("attendance_status", attendanceStatus.trim());
        }
        if (StringUtils.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like("name", keyword.trim()).or().like("specialty", keyword.trim()));
        }
        query.orderByAsc("department").orderByAsc("name");
        return doctorMapper.selectList(query);
    }

    public DoctorProfile getOwnDoctorProfile(Long userId) {
        DoctorProfile profile = doctorMapper.selectOne(new QueryWrapper<DoctorProfile>().eq("user_id", userId));
        if (profile == null) {
            throw new BizException("医生档案不存在");
        }
        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorProfile updateOwnDoctorProfile(Long userId, DoctorProfile payload) {
        DoctorProfile profile = getOwnDoctorProfile(userId);
        if (payload == null) {
            throw new BizException("医生档案不能为空");
        }
        if (StringUtils.isNotBlank(payload.getName())) {
            profile.setName(payload.getName().trim());
        }
        if (StringUtils.isNotBlank(payload.getDepartment())) {
            profile.setDepartment(payload.getDepartment().trim());
        }
        if (StringUtils.isNotBlank(payload.getTitle())) {
            profile.setTitle(payload.getTitle().trim());
        }
        profile.setSpecialty(StringUtils.defaultIfBlank(payload == null ? null : payload.getSpecialty(), "未填写"));
        validateDoctorProfile(profile);
        profile.setUpdatedAt(LocalDateTime.now());
        doctorMapper.updateById(profile);
        auditService.log("DOCTOR_PROFILE_SELF_UPDATE", "医生更新个人档案: user=" + userId);
        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorLeaveApplication submitLeave(Long userId, DoctorLeaveApplyRequest request) {
        DoctorProfile profile = getOwnDoctorProfile(userId);
        validateLeaveRequest(request);
        LocalDateTime now = LocalDateTime.now();
        DoctorLeaveApplication leave = new DoctorLeaveApplication();
        leave.setDoctorId(profile.getId());
        leave.setUserId(userId);
        leave.setStartTime(request.startTime());
        leave.setEndTime(request.endTime());
        leave.setReason(StringUtils.defaultIfBlank(request.reason(), "未填写"));
        leave.setStatus(request.startTime().isAfter(now) ? LEAVE_STATUS_WAITING : LEAVE_STATUS_ACTIVE);
        if (LEAVE_STATUS_ACTIVE.equals(leave.getStatus()) && !hasActiveLeave(profile.getId(), now)) {
            leave.setPreviousStatus(StringUtils.defaultIfBlank(profile.getAttendanceStatus(), DEFAULT_WORK_STATUS));
        }
        leave.setCreatedAt(now);
        leave.setUpdatedAt(now);
        leaveMapper.insert(leave);
        refreshDoctorLeaveStatus(profile.getId(), now);
        auditService.log("DOCTOR_LEAVE_SUBMIT", "医生提交请假: user=" + userId + "，时间: "
                + request.startTime() + " 至 " + request.endTime() + "，原因: " + leave.getReason());
        return leave;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorProfile resumeWork(Long userId) {
        DoctorProfile profile = getOwnDoctorProfile(userId);
        if (hasActiveLeave(profile.getId(), LocalDateTime.now())) {
            throw new BizException("当前仍有生效中的请假，不能手动恢复在诊");
        }
        profile.setAttendanceStatus(DEFAULT_WORK_STATUS);
        profile.setUpdatedAt(LocalDateTime.now());
        doctorMapper.updateById(profile);
        auditService.log("DOCTOR_RESUME_WORK", "医生恢复在诊: user=" + userId);
        return profile;
    }

    public List<DoctorLeaveApplication> listOwnLeaves(Long userId) {
        DoctorProfile profile = getOwnDoctorProfile(userId);
        refreshDoctorLeaveStatus(profile.getId(), LocalDateTime.now());
        return leaveMapper.selectList(new QueryWrapper<DoctorLeaveApplication>()
                .eq("doctor_id", profile.getId())
                .orderByDesc("start_time")
                .orderByDesc("created_at"));
    }

    public List<DoctorLeaveApplication> listLeaves(Long doctorId, String status,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        refreshAllDoctorLeaveStatuses();
        QueryWrapper<DoctorLeaveApplication> query = new QueryWrapper<>();
        if (doctorId != null) {
            query.eq("doctor_id", doctorId);
        }
        if (StringUtils.isNotBlank(status)) {
            query.eq("status", status.trim());
        }
        if (startTime != null && endTime != null) {
            query.lt("start_time", endTime).gt("end_time", startTime);
        } else if (startTime != null) {
            query.ge("end_time", startTime);
        } else if (endTime != null) {
            query.le("start_time", endTime);
        }
        query.orderByDesc("start_time").orderByDesc("created_at");
        return leaveMapper.selectList(query);
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorLeaveApplication saveLeaveByAdmin(DoctorLeaveApplication payload) {
        if (payload == null || payload.getDoctorId() == null) {
            throw new BizException("请选择请假医生");
        }
        validateLeaveRange(payload.getStartTime(), payload.getEndTime());
        DoctorProfile doctor = doctorMapper.selectById(payload.getDoctorId());
        if (doctor == null) {
            throw new BizException("医生不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        Long oldDoctorId = null;
        DoctorLeaveApplication leave;
        if (payload.getId() == null) {
            leave = new DoctorLeaveApplication();
            leave.setCreatedAt(now);
        } else {
            leave = leaveMapper.selectById(payload.getId());
            if (leave == null) {
                throw new BizException("请假记录不存在");
            }
            oldDoctorId = leave.getDoctorId();
        }
        leave.setDoctorId(payload.getDoctorId());
        leave.setUserId(doctor.getUserId());
        leave.setStartTime(payload.getStartTime());
        leave.setEndTime(payload.getEndTime());
        leave.setReason(StringUtils.defaultIfBlank(payload.getReason(), "未填写"));
        leave.setStatus(resolveLeaveStatus(payload.getStartTime(), payload.getEndTime(), now));
        if (LEAVE_STATUS_ACTIVE.equals(leave.getStatus()) && StringUtils.isBlank(leave.getPreviousStatus())) {
            leave.setPreviousStatus(StringUtils.defaultIfBlank(doctor.getAttendanceStatus(), DEFAULT_WORK_STATUS));
        }
        leave.setUpdatedAt(now);
        if (leave.getId() == null) {
            leaveMapper.insert(leave);
        } else {
            leaveMapper.updateById(leave);
        }
        if (oldDoctorId != null && !Objects.equals(oldDoctorId, leave.getDoctorId())) {
            refreshDoctorLeaveStatus(oldDoctorId, now);
        }
        refreshDoctorLeaveStatus(leave.getDoctorId(), now);
        auditService.log("DOCTOR_LEAVE_ADMIN_SAVE", "管理员保存请假: leave=" + leave.getId());
        return leave;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorLeaveApplication cancelOwnLeave(Long userId, Long leaveId) {
        DoctorProfile profile = getOwnDoctorProfile(userId);
        DoctorLeaveApplication leave = leaveMapper.selectById(leaveId);
        if (leave == null || !Objects.equals(leave.getDoctorId(), profile.getId())) {
            throw new BizException("请假记录不存在");
        }
        if (LEAVE_STATUS_FINISHED.equals(leave.getStatus()) || LEAVE_STATUS_CANCELLED.equals(leave.getStatus())) {
            throw new BizException("该请假记录不能撤销");
        }
        boolean wasActive = LEAVE_STATUS_ACTIVE.equals(leave.getStatus());
        LocalDateTime now = LocalDateTime.now();
        leave.setStatus(LEAVE_STATUS_CANCELLED);
        leave.setUpdatedAt(now);
        leaveMapper.updateById(leave);
        if (wasActive && !hasActiveLeave(profile.getId(), now) && REST_ATTENDANCE_STATUS.equals(profile.getAttendanceStatus())) {
            profile.setAttendanceStatus(StringUtils.defaultIfBlank(leave.getPreviousStatus(), DEFAULT_WORK_STATUS));
            profile.setUpdatedAt(now);
            doctorMapper.updateById(profile);
        } else {
            refreshDoctorLeaveStatus(profile.getId(), now);
        }
        auditService.log("DOCTOR_LEAVE_CANCEL", "医生撤销请假: user=" + userId + "，leave=" + leaveId);
        return leave;
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorLeaveApplication cancelLeaveByAdmin(Long leaveId) {
        DoctorLeaveApplication leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw new BizException("请假记录不存在");
        }
        if (LEAVE_STATUS_FINISHED.equals(leave.getStatus()) || LEAVE_STATUS_CANCELLED.equals(leave.getStatus())) {
            throw new BizException("该请假记录不能撤销");
        }
        boolean wasActive = LEAVE_STATUS_ACTIVE.equals(leave.getStatus());
        leave.setStatus(LEAVE_STATUS_CANCELLED);
        leave.setUpdatedAt(LocalDateTime.now());
        leaveMapper.updateById(leave);
        restoreAfterAdminLeaveRemoval(leave, wasActive);
        auditService.log("DOCTOR_LEAVE_ADMIN_CANCEL", "管理员撤销请假: leave=" + leaveId);
        return leave;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLeaveByAdmin(Long leaveId) {
        DoctorLeaveApplication leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw new BizException("请假记录不存在");
        }
        boolean wasActive = LEAVE_STATUS_ACTIVE.equals(leave.getStatus());
        leaveMapper.deleteById(leaveId);
        restoreAfterAdminLeaveRemoval(leave, wasActive);
        auditService.log("DOCTOR_LEAVE_ADMIN_DELETE", "管理员删除请假: leave=" + leaveId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAllDoctorLeaveStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<DoctorLeaveApplication> candidates = leaveMapper.selectList(new QueryWrapper<DoctorLeaveApplication>()
                .in("status", LEAVE_STATUS_WAITING, LEAVE_STATUS_ACTIVE));
        Set<Long> doctorIds = candidates.stream()
                .filter(leave -> shouldRefresh(leave, now))
                .map(DoctorLeaveApplication::getDoctorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        doctorIds.forEach(doctorId -> refreshDoctorLeaveStatus(doctorId, now));
    }

    @Transactional(rollbackFor = Exception.class)
    public DoctorProfile refreshDoctorLeaveStatus(Long doctorId, LocalDateTime now) {
        DoctorProfile profile = doctorMapper.selectById(doctorId);
        if (profile == null) {
            return null;
        }
        List<DoctorLeaveApplication> leaves = leaveMapper.selectList(new QueryWrapper<DoctorLeaveApplication>()
                .eq("doctor_id", doctorId)
                .in("status", LEAVE_STATUS_WAITING, LEAVE_STATUS_ACTIVE)
                .orderByAsc("start_time"));
        List<DoctorLeaveApplication> activated = new ArrayList<>();
        for (DoctorLeaveApplication leave : leaves) {
            if (LEAVE_STATUS_WAITING.equals(leave.getStatus()) && !leave.getStartTime().isAfter(now)) {
                boolean alreadyActive = leaves.stream()
                        .anyMatch(item -> !Objects.equals(item.getId(), leave.getId())
                                && LEAVE_STATUS_ACTIVE.equals(item.getStatus())
                                && !item.getStartTime().isAfter(now)
                                && !item.getEndTime().isBefore(now));
                leave.setStatus(LEAVE_STATUS_ACTIVE);
                if (!alreadyActive) {
                    leave.setPreviousStatus(StringUtils.defaultIfBlank(profile.getAttendanceStatus(), DEFAULT_WORK_STATUS));
                }
                leave.setUpdatedAt(now);
                leaveMapper.updateById(leave);
                activated.add(leave);
            }
            if (LEAVE_STATUS_ACTIVE.equals(leave.getStatus()) && leave.getEndTime().isBefore(now)) {
                leave.setStatus(LEAVE_STATUS_FINISHED);
                leave.setUpdatedAt(now);
                leaveMapper.updateById(leave);
            }
        }
        boolean hasActive = hasActiveLeave(doctorId, now);
        if (hasActive) {
            if (!REST_ATTENDANCE_STATUS.equals(profile.getAttendanceStatus())) {
                profile.setAttendanceStatus(REST_ATTENDANCE_STATUS);
                profile.setUpdatedAt(now);
                doctorMapper.updateById(profile);
            }
            return profile;
        }
        if (REST_ATTENDANCE_STATUS.equals(profile.getAttendanceStatus())) {
            String restoreStatus = findRestoreStatus(doctorId, activated);
            profile.setAttendanceStatus(restoreStatus);
            profile.setUpdatedAt(now);
            doctorMapper.updateById(profile);
        }
        return profile;
    }

    public PageResponse<DoctorScheduleView> scheduleQuery(String department, String doctorName, String date,
                                                          boolean availableOnly, long page, long size) {
        String queryDepartment = StringUtils.trimToNull(department);
        long safePage = Math.max(page, 1);
        safePage = PageSupport.page(safePage);
        long safeSize = PageSupport.size(size);
        Page<Object> pageParam = new Page<>(safePage, safeSize);
        List<DoctorScheduleView> records = scheduleMapper.selectSchedulePage(pageParam, queryDepartment, doctorName, date, availableOnly);
        long total = scheduleMapper.countSchedulePage(queryDepartment, doctorName, date, availableOnly);
        // 剩余号源只基于当前页排班批量统计，避免对全部挂号记录做全表聚合。
        List<DoctorScheduleView> recordsWithRemain = fillScheduleRemain(records);
        return new PageResponse<>(safePage, safeSize, total, recordsWithRemain);
    }

    private void validateLeaveRequest(DoctorLeaveApplyRequest request) {
        if (request == null) {
            throw new BizException("请假申请不能为空");
        }
        validateLeaveRange(request.startTime(), request.endTime());
    }

    private void validateLeaveRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BizException("请选择请假开始和结束时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BizException("请假结束时间必须晚于开始时间");
        }
        if (endTime.isBefore(LocalDateTime.now())) {
            throw new BizException("请假结束时间不能早于当前时间");
        }
    }

    private String resolveLeaveStatus(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        if (endTime.isBefore(now)) {
            return LEAVE_STATUS_FINISHED;
        }
        if (startTime.isAfter(now)) {
            return LEAVE_STATUS_WAITING;
        }
        return LEAVE_STATUS_ACTIVE;
    }

    private boolean shouldRefresh(DoctorLeaveApplication leave, LocalDateTime now) {
        if (leave == null || leave.getStartTime() == null || leave.getEndTime() == null) {
            return false;
        }
        if (LEAVE_STATUS_WAITING.equals(leave.getStatus())) {
            return !leave.getStartTime().isAfter(now);
        }
        if (LEAVE_STATUS_ACTIVE.equals(leave.getStatus())) {
            return true;
        }
        return false;
    }

    private boolean hasActiveLeave(Long doctorId, LocalDateTime now) {
        return leaveMapper.selectCount(new QueryWrapper<DoctorLeaveApplication>()
                .eq("doctor_id", doctorId)
                .eq("status", LEAVE_STATUS_ACTIVE)
                .le("start_time", now)
                .ge("end_time", now)) > 0;
    }

    private void restoreAfterAdminLeaveRemoval(DoctorLeaveApplication leave, boolean wasActive) {
        LocalDateTime now = LocalDateTime.now();
        DoctorProfile profile = doctorMapper.selectById(leave.getDoctorId());
        if (profile == null) {
            return;
        }
        if (wasActive && !hasActiveLeave(leave.getDoctorId(), now) && REST_ATTENDANCE_STATUS.equals(profile.getAttendanceStatus())) {
            profile.setAttendanceStatus(StringUtils.defaultIfBlank(leave.getPreviousStatus(), DEFAULT_WORK_STATUS));
            profile.setUpdatedAt(now);
            doctorMapper.updateById(profile);
            return;
        }
        refreshDoctorLeaveStatus(leave.getDoctorId(), now);
    }

    private String findRestoreStatus(Long doctorId, List<DoctorLeaveApplication> activated) {
        return leaveMapper.selectList(new QueryWrapper<DoctorLeaveApplication>()
                        .eq("doctor_id", doctorId)
                        .eq("status", LEAVE_STATUS_FINISHED)
                        .isNotNull("previous_status")
                        .orderByDesc("end_time"))
                .stream()
                .map(DoctorLeaveApplication::getPreviousStatus)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .or(() -> activated.stream()
                        .sorted(Comparator.comparing(DoctorLeaveApplication::getStartTime))
                        .map(DoctorLeaveApplication::getPreviousStatus)
                        .filter(StringUtils::isNotBlank)
                        .findFirst())
                .orElse(DEFAULT_WORK_STATUS);
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
        boolean resting = !isDoctorAvailable(record.attendanceStatus())
                || (record.status() != null && record.status() == 0)
                || hasLeaveConflict(record.doctorId(), record.scheduleDate(), record.shift());
        int remain = resting
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
                resting ? "休息" : "正常",
                record.level(),
                record.limit(),
                remain,
                record.status()
        );
    }

    private boolean isDoctorAvailable(String attendanceStatus) {
        return ACTIVE_ATTENDANCE_STATUSES.contains(attendanceStatus);
    }

    private boolean hasLeaveConflict(Long doctorId, String scheduleDate, String shift) {
        if (doctorId == null || StringUtils.isBlank(scheduleDate)) {
            return false;
        }
        LocalDate date = LocalDate.parse(scheduleDate);
        LocalDateTime slotStart = date.atTime(shiftStart(shift));
        LocalDateTime slotEnd = date.atTime(shiftEnd(shift));
        return leaveMapper.selectCount(new QueryWrapper<DoctorLeaveApplication>()
                .eq("doctor_id", doctorId)
                .in("status", LEAVE_STATUS_WAITING, LEAVE_STATUS_ACTIVE)
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

    public PageResponse<Prescription> listPrescriptions(long page, long size) {
        Page<Prescription> pageParam = new Page<>(PageSupport.page(page), PageSupport.size(size));
        QueryWrapper<Prescription> query = new QueryWrapper<>();
        query.orderByDesc("created_at");
        prescriptionMapper.selectPage(pageParam, query);
        return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal(), pageParam.getRecords());
    }

    private void validateDoctorProfile(DoctorProfile doctor) {
        if (doctor == null) {
            throw new BizException("医生档案不能为空");
        }
        if (StringUtils.isBlank(doctor.getName())) {
            throw new BizException("请输入医生姓名");
        }
        if (StringUtils.isBlank(doctor.getDepartment())) {
            throw new BizException("请选择医生所属科室");
        }
        if (StringUtils.isBlank(doctor.getTitle())) {
            throw new BizException("请选择医生职称");
        }
        if (StringUtils.isBlank(doctor.getAttendanceStatus())) {
            throw new BizException("请选择医生在岗状态");
        }
        if (StringUtils.isBlank(doctor.getSpecialty())) {
            doctor.setSpecialty("未填写");
        }
    }
}
