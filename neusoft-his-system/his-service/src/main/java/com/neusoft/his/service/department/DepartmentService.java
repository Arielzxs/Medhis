package com.neusoft.his.service.department;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.his.common.audit.AuditService;
import com.neusoft.his.common.exception.BizException;
import com.neusoft.his.dal.entity.Department;
import com.neusoft.his.dal.entity.DoctorProfile;
import com.neusoft.his.dal.mapper.DepartmentMapper;
import com.neusoft.his.dal.mapper.DoctorProfileMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final AuditService auditService;

    public DepartmentService(DepartmentMapper departmentMapper, DoctorProfileMapper doctorProfileMapper,
                             AuditService auditService) {
        this.departmentMapper = departmentMapper;
        this.doctorProfileMapper = doctorProfileMapper;
        this.auditService = auditService;
    }

    public List<Department> list(String keyword, Boolean enabledOnly) {
        QueryWrapper<Department> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like("name", keyword.trim())
                    .or()
                    .like("code", keyword.trim())
                    .or()
                    .like("location", keyword.trim()));
        }
        if (Boolean.TRUE.equals(enabledOnly)) {
            query.eq("enabled", 1);
        }
        query.orderByAsc("sort_no").orderByAsc("name");
        return departmentMapper.selectList(query);
    }

    @Transactional(rollbackFor = Exception.class)
    public Department save(Department payload) {
        if (payload == null || StringUtils.isBlank(payload.getName())) {
            throw new BizException("科室名称不能为空");
        }
        String name = payload.getName().trim();
        QueryWrapper<Department> duplicateQuery = new QueryWrapper<>();
        duplicateQuery.eq("name", name);
        if (payload.getId() != null) {
            duplicateQuery.ne("id", payload.getId());
        }
        if (departmentMapper.selectCount(duplicateQuery) > 0) {
            throw new BizException("科室名称已存在");
        }

        Department department = payload.getId() == null ? new Department() : departmentMapper.selectById(payload.getId());
        if (department == null) {
            throw new BizException("科室不存在");
        }
        department.setName(name);
        department.setCode(StringUtils.defaultIfBlank(payload.getCode(), "").trim());
        department.setLocation(StringUtils.defaultIfBlank(payload.getLocation(), "").trim());
        department.setDescription(StringUtils.defaultIfBlank(payload.getDescription(), "").trim());
        department.setSortNo(payload.getSortNo() == null ? 100 : payload.getSortNo());
        department.setEnabled(payload.getEnabled() == null ? 1 : payload.getEnabled());
        department.setUpdatedAt(LocalDateTime.now());
        if (department.getId() == null) {
            department.setCreatedAt(LocalDateTime.now());
            departmentMapper.insert(department);
        } else {
            departmentMapper.updateById(department);
        }
        auditService.log("DEPARTMENT_SAVE", "保存科室: " + department.getName());
        return department;
    }

    @Transactional(rollbackFor = Exception.class)
    public Department updateEnabled(Long id, boolean enabled) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BizException("科室不存在");
        }
        department.setEnabled(enabled ? 1 : 0);
        department.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(department);
        auditService.log("DEPARTMENT_STATUS", (enabled ? "启用" : "停用") + "科室: " + department.getName());
        return department;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BizException("科室不存在");
        }
        long usedCount = doctorProfileMapper.selectCount(new QueryWrapper<DoctorProfile>()
                .eq("department", department.getName()));
        if (usedCount > 0) {
            throw new BizException("该科室已有医生档案，不能删除，可先停用");
        }
        departmentMapper.deleteById(id);
        auditService.log("DEPARTMENT_DELETE", "删除科室: " + department.getName());
    }
}
