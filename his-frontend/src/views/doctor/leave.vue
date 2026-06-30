<template>
  <div class="leave-container">
    <el-card shadow="never" class="leave-card">
      <div class="header-action">
        <div>
          <span class="title">{{ isAdmin ? "请假记录维护" : "我的请假" }}</span>
          <p class="subtitle">{{ isAdmin ? "维护医生请假时间段，避免休息期间被挂号" : "提交请假申请并查看自己的请假记录" }}</p>
        </div>
        <div class="header-buttons">
          <el-button v-if="isAdmin" type="primary" icon="Plus" @click="openAdminLeaveDialog()">新增请假</el-button>
          <el-button v-else type="warning" icon="User" @click="openLeaveDialog">提交请假</el-button>
        </div>
      </div>

      <template v-if="isAdmin">
        <el-form :inline="true" :model="adminLeaveQuery" class="filter-box">
          <el-form-item label="医生">
            <el-select v-model="adminLeaveQuery.doctorId" placeholder="全部医生" clearable filterable style="width: 180px">
              <el-option v-for="doctor in doctorOptions" :key="doctor.id" :label="doctor.name" :value="doctor.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="adminLeaveQuery.status" placeholder="全部状态" clearable style="width: 140px">
              <el-option v-for="item in leaveStatusOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间">
            <el-date-picker
              v-model="adminLeaveQuery.range"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 330px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="fetchAdminLeaves">查询</el-button>
            <el-button @click="resetAdminLeaveQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="pagedAdminLeaveRows" border stripe style="width: 100%" v-loading="loadingLeaves">
          <el-table-column prop="doctorName" label="医生" align="center" width="120" />
          <el-table-column prop="startTime" label="开始时间" align="center" width="180" />
          <el-table-column prop="endTime" label="结束时间" align="center" width="180" />
          <el-table-column prop="reason" label="原因" header-align="center" min-width="180" />
          <el-table-column prop="status" label="状态" align="center" width="100">
            <template #default="{ row }">
              <el-tag :type="leaveStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="190" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openAdminLeaveDialog(row)">编辑</el-button>
              <el-popconfirm v-if="canCancelLeave(row)" title="确认撤销该请假？" @confirm="adminCancelLeave(row.id)">
                <template #reference>
                  <el-button type="warning" link size="small">撤销</el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm title="确认删除该请假记录？" @confirm="adminDeleteLeave(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="adminLeavePage.page"
            v-model:page-size="adminLeavePage.size"
            :page-sizes="[10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="adminLeaveRows.length"
          />
        </div>
      </template>

      <template v-else>
        <div class="status-line">
          <span>当前状态</span>
          <el-tag :type="statusType(myProfile.attendanceStatus)" effect="light">
            {{ myProfile.attendanceStatus || "待完善" }}
          </el-tag>
        </div>

        <el-table :data="pagedLeaveApplications" border stripe style="width: 100%" v-loading="loadingLeaves">
          <el-table-column prop="startTime" label="开始时间" align="center" width="180" />
          <el-table-column prop="endTime" label="结束时间" align="center" width="180" />
          <el-table-column prop="reason" label="原因" header-align="center" min-width="180" />
          <el-table-column prop="status" label="状态" align="center" width="100">
            <template #default="{ row }">
              <el-tag :type="leaveStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="100">
            <template #default="{ row }">
              <el-popconfirm
                v-if="canCancelLeave(row)"
                title="确认撤销该请假？"
                @confirm="cancelLeave(row.id)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">撤销</el-button>
                </template>
              </el-popconfirm>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="myLeavePage.page"
            v-model:page-size="myLeavePage.size"
            :page-sizes="[10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="leaveApplications.length"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="leaveDialogVisible" title="提交请假" width="520px" destroy-on-close>
      <el-form ref="leaveFormRef" :model="leaveForm" :rules="leaveRules" label-width="84px">
        <el-form-item label="请假时间" prop="range">
          <el-date-picker
            v-model="leaveForm.range"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model.trim="leaveForm.reason" type="textarea" :rows="4" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitLeave">提交请假</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminLeaveDialogVisible" :title="adminLeaveForm.id ? '编辑请假记录' : '新增请假记录'" width="560px" destroy-on-close>
      <el-form ref="adminLeaveFormRef" :model="adminLeaveForm" :rules="adminLeaveRules" label-width="84px">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="adminLeaveForm.doctorId" placeholder="请选择医生" filterable style="width: 100%">
            <el-option v-for="doctor in doctorOptions" :key="doctor.id" :label="`${doctor.name}｜${doctor.department || '未分科'}`" :value="doctor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间" prop="range">
          <el-date-picker
            v-model="adminLeaveForm.range"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model.trim="adminLeaveForm.reason" type="textarea" :rows="4" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminLeaveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdminLeave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { useUserStore } from "../../store/user";

const userStore = useUserStore();
const isAdmin = computed(() => userStore.roles.includes("ADMIN"));

const leaveStatusOptions = ["待生效", "生效中", "已结束", "已撤销"];

const loadingLeaves = ref(false);
const submitting = ref(false);
const leaveDialogVisible = ref(false);
const adminLeaveDialogVisible = ref(false);
const leaveFormRef = ref();
const adminLeaveFormRef = ref();
const doctorOptions = ref([]);
const adminLeaves = ref([]);
const leaveApplications = ref([]);
const adminLeavePage = reactive({
  page: 1,
  size: 10,
});
const myLeavePage = reactive({
  page: 1,
  size: 10,
});

const myProfile = reactive({
  attendanceStatus: "",
});

const leaveForm = reactive({
  range: [],
  reason: "",
});

const adminLeaveQuery = reactive({
  doctorId: null,
  status: "",
  range: [],
});

const adminLeaveForm = reactive({
  id: null,
  doctorId: null,
  range: [],
  reason: "",
});

const leaveRules = {
  range: [{ required: true, message: "请选择请假时间段", trigger: "change" }],
};
const adminLeaveRules = {
  doctorId: [{ required: true, message: "请选择医生", trigger: "change" }],
  range: [{ required: true, message: "请选择请假时间段", trigger: "change" }],
};

const doctorMap = computed(() => {
  const map = {};
  doctorOptions.value.forEach((doctor) => {
    map[doctor.id] = doctor;
  });
  return map;
});

const adminLeaveRows = computed(() =>
  adminLeaves.value.map((leave) => ({
    ...leave,
    doctorName: doctorMap.value[leave.doctorId]?.name || `医生#${leave.doctorId}`,
  })),
);

const pagedAdminLeaveRows = computed(() => {
  const start = (adminLeavePage.page - 1) * adminLeavePage.size;
  return adminLeaveRows.value.slice(start, start + adminLeavePage.size);
});

const pagedLeaveApplications = computed(() => {
  const start = (myLeavePage.page - 1) * myLeavePage.size;
  return leaveApplications.value.slice(start, start + myLeavePage.size);
});

const fetchDoctors = async () => {
  const data = await request.get("/api/doctors/profiles");
  doctorOptions.value = data || [];
};

const fetchAdminLeaves = async () => {
  loadingLeaves.value = true;
  try {
    const [startTime, endTime] = adminLeaveQuery.range || [];
    const data = await request.get("/api/doctors/leaves", {
      params: {
        doctorId: adminLeaveQuery.doctorId || undefined,
        status: adminLeaveQuery.status || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined,
      },
    });
    adminLeaves.value = data || [];
    adminLeavePage.page = 1;
  } finally {
    loadingLeaves.value = false;
  }
};

const fetchMyProfile = async () => {
  const data = await request.get("/api/doctors/me/profile");
  myProfile.attendanceStatus = data?.attendanceStatus || "";
};

const fetchMyLeaves = async () => {
  loadingLeaves.value = true;
  try {
    leaveApplications.value = (await request.get("/api/doctors/me/leaves")) || [];
    myLeavePage.page = 1;
  } finally {
    loadingLeaves.value = false;
  }
};

const resetAdminLeaveQuery = () => {
  adminLeaveQuery.doctorId = null;
  adminLeaveQuery.status = "";
  adminLeaveQuery.range = [];
  adminLeavePage.page = 1;
  fetchAdminLeaves();
};

const openLeaveDialog = () => {
  leaveForm.range = [];
  leaveForm.reason = "";
  leaveDialogVisible.value = true;
};

const openAdminLeaveDialog = (row) => {
  Object.assign(adminLeaveForm, {
    id: row?.id || null,
    doctorId: row?.doctorId || null,
    range: row ? [row.startTime, row.endTime] : [],
    reason: row?.reason || "",
  });
  adminLeaveDialogVisible.value = true;
};

const submitLeave = async () => {
  await leaveFormRef.value?.validate();
  const [startTime, endTime] = leaveForm.range || [];
  if (!startTime || !endTime) return ElMessage.warning("请选择请假时间段");
  submitting.value = true;
  try {
    await request.post("/api/doctors/me/leave", {
      startTime,
      endTime,
      reason: leaveForm.reason,
    });
    await Promise.all([fetchMyProfile(), fetchMyLeaves()]);
    ElMessage.success("请假申请已提交");
    leaveDialogVisible.value = false;
  } finally {
    submitting.value = false;
  }
};

const canCancelLeave = (leave) => ["待生效", "生效中"].includes(leave?.status);

const cancelLeave = async (id) => {
  submitting.value = true;
  try {
    await request.post(`/api/doctors/me/leaves/${id}/cancel`);
    await Promise.all([fetchMyProfile(), fetchMyLeaves()]);
    ElMessage.success("请假已撤销");
  } finally {
    submitting.value = false;
  }
};

const submitAdminLeave = async () => {
  await adminLeaveFormRef.value?.validate();
  const [startTime, endTime] = adminLeaveForm.range || [];
  if (!startTime || !endTime) return ElMessage.warning("请选择请假时间段");
  submitting.value = true;
  try {
    const payload = {
      doctorId: adminLeaveForm.doctorId,
      startTime,
      endTime,
      reason: adminLeaveForm.reason,
    };
    if (adminLeaveForm.id) {
      await request.put(`/api/doctors/leaves/${adminLeaveForm.id}`, payload);
    } else {
      await request.post("/api/doctors/leaves", payload);
    }
    ElMessage.success("请假记录已保存");
    adminLeaveDialogVisible.value = false;
    await Promise.all([fetchDoctors(), fetchAdminLeaves()]);
  } finally {
    submitting.value = false;
  }
};

const adminCancelLeave = async (id) => {
  await request.post(`/api/doctors/leaves/${id}/cancel`);
  ElMessage.success("请假已撤销");
  await Promise.all([fetchDoctors(), fetchAdminLeaves()]);
};

const adminDeleteLeave = async (id) => {
  await request.delete(`/api/doctors/leaves/${id}`);
  ElMessage.success("请假记录已删除");
  await Promise.all([fetchDoctors(), fetchAdminLeaves()]);
};

const leaveStatusType = (status) => {
  if (status === "生效中") return "warning";
  if (status === "待生效") return "primary";
  if (status === "已撤销") return "danger";
  if (status === "已结束") return "info";
  return "info";
};

const statusType = (status) => {
  if (status === "在岗" || status === "在诊") return "success";
  if (status === "待完善") return "warning";
  if (status === "休息" || status === "停诊") return "info";
  if (status === "离职") return "danger";
  return "info";
};

onMounted(async () => {
  if (isAdmin.value) {
    await fetchDoctors();
    await fetchAdminLeaves();
  } else {
    await Promise.all([fetchMyProfile(), fetchMyLeaves()]);
  }
});
</script>

<style scoped>
.leave-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: #f0f2f5;
}

.leave-card {
  min-height: 720px;
  border: none;
  border-radius: 8px;
}

.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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

.header-buttons {
  display: flex;
  gap: 10px;
}

.filter-box {
  margin-bottom: 20px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.text-muted {
  color: #a8abb2;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
