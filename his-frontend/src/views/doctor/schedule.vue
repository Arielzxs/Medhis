<!-- src/views/doctor/schedule.vue -->
<template>
  <div class="schedule-container">
    <el-card shadow="never" class="schedule-card">
      <div class="header-action">
        <div>
          <span class="title">人员排班管理</span>
          <p class="subtitle">维护医生出诊日期、班次、号别与挂号限额</p>
        </div>
        <el-button type="primary" @click="handleAdd">新增排班</el-button>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="出诊科室">
            <el-select
              v-model="queryParams.department"
              placeholder="请选择科室"
              clearable
              style="width: 170px"
            >
              <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="医生姓名">
            <el-input
              v-model="queryParams.doctorName"
              placeholder="请输入医生姓名"
              clearable
              style="width: 170px"
            />
          </el-form-item>
          <el-form-item label="排班日期">
            <el-date-picker
              v-model="queryParams.date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 170px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="scheduleDate" label="排班日期" align="center" width="130" />
        <el-table-column prop="shift" label="班次" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="shiftTag(row.shift)" effect="plain">{{ row.shift }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="出诊科室" align="center" />
        <el-table-column prop="doctorName" label="医生姓名" align="center" />
        <el-table-column prop="level" label="号别" align="center" width="110" />
        <el-table-column prop="limit" label="挂号限额" align="center" width="100" />
        <el-table-column prop="remain" label="剩余号源" align="center" width="100" />
        <el-table-column prop="status" label="状态" align="center" width="130">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="正常"
              inactive-text="停诊"
              inline-prompt
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该排班？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="pageState.page"
          v-model:page-size="pageState.size"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pageState.total"
          @current-change="fetchSchedules"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑排班' : '新增排班'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="出诊医生" prop="doctorId">
          <el-select
            v-model="form.doctorId"
            placeholder="请选择医生"
            filterable
            style="width: 100%"
            @change="syncDoctorDefaults"
          >
            <el-option
              v-for="doctor in doctorOptions"
              :key="doctor.id"
              :label="`${doctor.name}｜${doctor.department || '未分科'}｜${doctor.title || '医师'}`"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排班日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="班次" prop="shift">
          <el-radio-group v-model="form.shift">
            <el-radio-button label="上午" />
            <el-radio-button label="下午" />
            <el-radio-button label="全天" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="号别" prop="level">
          <el-select v-model="form.level" placeholder="请选择号别" style="width: 100%">
            <el-option label="普通号" value="普通号" />
            <el-option label="专家号" value="专家号" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂号限额" prop="registrationLimit">
          <el-input-number v-model="form.registrationLimit" :min="0" :max="300" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">停诊</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { fetchDepartmentOptions } from "../../utils/departments";

const departments = ref([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const doctorOptions = ref([]);
const tableData = ref([]);
const pageState = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const queryParams = reactive({
  department: "",
  doctorName: "",
  date: "",
});

const form = reactive({
  id: null,
  doctorId: null,
  scheduleDate: "",
  shift: "上午",
  level: "普通号",
  registrationLimit: 50,
  status: 1,
});

const rules = {
  doctorId: [{ required: true, message: "请选择医生", trigger: "change" }],
  scheduleDate: [{ required: true, message: "请选择排班日期", trigger: "change" }],
  shift: [{ required: true, message: "请选择班次", trigger: "change" }],
  level: [{ required: true, message: "请选择号别", trigger: "change" }],
  registrationLimit: [{ required: true, message: "请输入挂号限额", trigger: "blur" }],
};

const fetchDoctors = async () => {
  doctorOptions.value = await request.get("/api/doctors", {
    params: { department: queryParams.department },
  });
};

const fetchSchedules = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/doctors/schedules", {
      params: {
        department: queryParams.department,
        doctorName: queryParams.doctorName,
        date: queryParams.date,
        page: pageState.page,
        size: pageState.size,
      },
    });
    tableData.value = res.records || [];
    pageState.total = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, {
    id: null,
    doctorId: null,
    scheduleDate: queryParams.date || "",
    shift: "上午",
    level: "普通号",
    registrationLimit: 50,
    status: 1,
  });
};

const syncDoctorDefaults = () => {
  const doctor = doctorOptions.value.find((item) => item.id === form.doctorId);
  if (doctor?.title?.includes("主任")) {
    form.level = "专家号";
  }
};

const shiftTag = (shift) => {
  if (shift === "上午") return "success";
  if (shift === "下午") return "warning";
  return "info";
};

const handleSearch = async () => {
  pageState.page = 1;
  await Promise.all([fetchDoctors(), fetchSchedules()]);
};

const handlePageSizeChange = () => {
  pageState.page = 1;
  fetchSchedules();
};

const resetQuery = () => {
  queryParams.department = departments.value[0] || "";
  queryParams.doctorName = "";
  queryParams.date = "";
  handleSearch();
};

const handleAdd = async () => {
  resetForm();
  await fetchDoctors();
  dialogVisible.value = true;
};

const handleEdit = async (row) => {
  await fetchDoctors();
  Object.assign(form, {
    id: row.id,
    doctorId: row.doctorId,
    scheduleDate: row.scheduleDate,
    shift: row.shift,
    level: row.level || "普通号",
    registrationLimit: row.limit ?? 50,
    status: row.status ?? 1,
  });
  dialogVisible.value = true;
};

const submitSchedule = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = {
      doctorId: form.doctorId,
      scheduleDate: form.scheduleDate,
      shift: form.shift,
      level: form.level,
      registrationLimit: form.registrationLimit,
      status: form.status,
    };
    if (form.id) {
      await request.put(`/api/doctors/schedules/${form.id}`, payload);
    } else {
      await request.post("/api/doctors/schedules", payload);
    }
    ElMessage.success("排班保存成功");
    dialogVisible.value = false;
    fetchSchedules();
  } finally {
    saving.value = false;
  }
};

const handleStatusChange = async (row) => {
  await request.put(`/api/doctors/schedules/${row.id}`, {
    doctorId: row.doctorId,
    scheduleDate: row.scheduleDate,
    shift: row.shift,
    level: row.level,
    registrationLimit: row.limit,
    status: row.status,
  });
  ElMessage.success(row.status === 1 ? "已恢复正常出诊" : "已设为停诊");
  fetchSchedules();
};

const handleDelete = async (id) => {
  await request.delete(`/api/doctors/schedules/${id}`);
  ElMessage.success("排班已删除");
  fetchSchedules();
};

onMounted(async () => {
  departments.value = await fetchDepartmentOptions();
  if (!queryParams.department) {
    queryParams.department = departments.value[0] || "";
  }
  handleSearch();
});
</script>

<style scoped>
.schedule-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: #f0f2f5;
}

.schedule-card {
  min-height: 750px;
  border: none;
  border-radius: 10px;
}

.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.filter-box {
  margin-bottom: 20px;
}

.pagination-box {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
