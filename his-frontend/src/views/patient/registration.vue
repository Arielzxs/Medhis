<template>
  <div class="registration-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never" class="info-card">
          <template #header>
            <div class="card-header">
              <span class="title">患者建档与查询</span>
            </div>
          </template>

          <div class="search-bar">
            <el-input
              v-model="searchIdCard"
              placeholder="请输入患者身份证号检索"
              class="input-with-select"
              clearable
              :disabled="patientLocked"
            >
              <template #append>
                <el-button icon="Search" @click="handleSearchPatient"
                  >读取档案</el-button
                >
              </template>
            </el-input>
          </div>

          <el-form
            :model="patientForm"
            label-width="90px"
            label-position="left"
            class="patient-form"
          >
            <el-form-item label="姓名">
              <el-input
                v-model="patientForm.name"
                placeholder="请输入姓名"
                :disabled="patientLocked"
              />
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input
                v-model="patientForm.idCard"
                placeholder="请输入身份证号"
                :disabled="patientLocked"
              >
                <template #append>
                  <el-button
                    :loading="idCardParsing"
                    @click="parseIdCard"
                    :disabled="patientLocked || !patientForm.idCard || patientForm.idCard.length !== 18"
                  >
                    识别生日
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="patientForm.gender" :disabled="patientLocked">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="patientForm.birthday"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                :disabled="patientLocked"
              />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input
                v-model="patientForm.phone"
                placeholder="请输入手机号"
                :disabled="patientLocked"
              />
            </el-form-item>
            <el-divider border-style="dashed" />
            <el-form-item label="就诊卡余额">
              <span class="balance-text"
                >¥ {{ Number(patientForm.balance || 0).toFixed(2) }}</span
              >
              <el-button
                type="primary"
                link
                style="margin-left: 15px"
                :disabled="!patientLocked"
                @click="openRechargeDialog"
                >充值</el-button
              >
              <el-button
                type="danger"
                link
                :disabled="!patientLocked || Number(patientForm.balance || 0) <= 0"
                @click="openBalanceRefundDialog"
                >退费</el-button
              >
            </el-form-item>

            <div class="form-actions">
              <el-button
                type="primary"
                plain
                style="flex: 1"
                :disabled="patientLocked"
                @click="savePatient"
                >保存 / 更新档案</el-button
              >
              <el-button
                type="success"
                plain
                style="flex: 1"
                :disabled="patientLocked || !patientForm.name || !patientForm.idCard"
                @click="confirmPatient"
                >确认当前患者</el-button
              >
              <el-button
                v-if="patientLocked"
                plain
                style="flex: 1"
                @click="unlockPatient"
                >更换患者</el-button
              >
            </div>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="schedule-card">
          <template #header>
            <div class="card-header">
              <span class="title">号源查询与挂号办理</span>
            </div>
          </template>

          <el-form
            :inline="true"
            :model="scheduleQuery"
            class="schedule-filter"
          >
            <el-form-item label="挂号科室">
              <el-select
                v-model="scheduleQuery.department"
                placeholder="选择科室"
                style="width: 140px"
              >
                <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="看诊医生">
              <el-input
                v-model="scheduleQuery.doctorName"
                placeholder="输入医生名"
                style="width: 120px"
              />
            </el-form-item>
            <el-form-item label="看诊日期">
              <el-date-picker
                v-model="scheduleQuery.date"
                type="date"
                placeholder="选择日期"
                style="width: 140px"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="scheduleLoading"
                @click="fetchSchedules(true)"
              >
                查询排班
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            :data="scheduleList"
            v-loading="scheduleLoading"
            border
            stripe
            row-key="id"
            @selection-change="handleSelectionChange"
            style="width: 100%"
            height="450"
          >
            <el-table-column type="selection" width="48" align="center" :selectable="isScheduleSelectable" />
            <el-table-column
              prop="doctorName"
              label="医生姓名"
              align="center"
              width="100"
            />
            <el-table-column
              prop="title"
              label="职称"
              align="center"
              width="100"
            />
            <el-table-column prop="department" label="科室" align="center" />
            <el-table-column
              prop="attendanceStatus"
              label="医生状态"
              align="center"
              width="100"
            >
              <template #default="scope">
                <el-tag :type="doctorStatusType(scope.row)" effect="light">
                  {{ doctorStatusText(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="scheduleDate"
              label="就诊日期"
              align="center"
              width="120"
            />
            <el-table-column
              prop="visitTime"
              label="班次时间"
              align="center"
              width="170"
            />
            <el-table-column
              prop="fee"
              label="挂号费(元)"
              align="center"
              width="100"
            >
              <template #default="scope">
                <span class="fee-text">¥{{ scope.row.fee }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="remain"
              label="剩余号源"
              align="center"
              width="100"
            >
              <template #default="scope">
                <el-tag :type="scope.row.remain > 0 ? 'success' : 'danger'">
                  {{ scope.row.remain > 0 ? scope.row.remain : "满号" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-pagination">
            <el-pagination
              v-model:current-page="schedulePage.page"
              v-model:page-size="schedulePage.size"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="schedulePage.total"
              @current-change="fetchSchedules"
              @size-change="handleSchedulePageSizeChange"
              :disabled="scheduleLoading"
            />
          </div>

          <div class="bottom-action">
            <div class="summary">
              已选医生：<span class="highlight">{{ selectedSchedules.length }} 位</span>
              需支付金额：<span class="highlight fee-text"
                >¥ {{ totalFee.toFixed(2) }}</span
              >
            </div>
            <el-button
              type="success"
              size="large"
              :disabled="scheduleLoading || !selectedSchedules.length || !patientLocked"
              @click="submitRegistration"
            >
              确认挂号并扣费
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="rechargeVisible"
      title="就诊卡充值"
      width="420px"
      destroy-on-close
    >
      <el-form :model="rechargeForm" label-width="90px">
        <el-form-item label="当前患者">
          <span>{{ patientForm.name }} / {{ patientForm.idCard }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span class="balance-text">¥ {{ Number(patientForm.balance || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="充值金额">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="1"
            :precision="2"
            :step="50"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeLoading" @click="submitRecharge"
          >确认充值</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="balanceRefundVisible"
      title="就诊卡余额退费"
      width="420px"
      destroy-on-close
    >
      <el-form :model="balanceRefundForm" label-width="90px">
        <el-form-item label="当前患者">
          <span>{{ patientForm.name }} / {{ patientForm.idCard }}</span>
        </el-form-item>
        <el-form-item label="当前余额">
          <span class="balance-text">¥ {{ Number(patientForm.balance || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="退费金额">
          <el-input-number
            v-model="balanceRefundForm.amount"
            :min="0.01"
            :max="Number(patientForm.balance || 0)"
            :precision="2"
            :step="50"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退费原因">
          <el-input
            v-model="balanceRefundForm.reason"
            type="textarea"
            :rows="2"
            placeholder="请输入退费原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="balanceRefundVisible = false">取消</el-button>
        <el-button type="danger" :loading="balanceRefundLoading" @click="submitBalanceRefund"
          >确认退费</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { fetchDepartmentOptions } from "../../utils/departments";

// ===== 患者信息逻辑 =====
const searchIdCard = ref("");
const patientForm = reactive({
  id: null,
  name: "",
  idCard: "",
  gender: "男",
  birthday: "",
  phone: "",
  balance: 0.0,
});

const idCardParsing = ref(false);
const patientLocked = ref(false);
const rechargeVisible = ref(false);
const rechargeLoading = ref(false);
const rechargeForm = reactive({ amount: 100 });
const balanceRefundVisible = ref(false);
const balanceRefundLoading = ref(false);
const balanceRefundForm = reactive({ amount: 100, reason: "" });

const applyPatient = (patient) => {
  Object.assign(patientForm, {
    id: patient.id,
    name: patient.name || "",
    idCard: patient.idCard || "",
    gender: patient.gender || "未知",
    birthday: patient.birthday || "",
    phone: patient.phone || "",
    balance: Number(patient.balance || 0),
  });
};

const parseIdCard = async () => {
  const idCard = patientForm.idCard;
  if (!idCard || idCard.length !== 18) {
    return ElMessage.warning("请输入 18 位身份证号");
  }
  idCardParsing.value = true;
  try {
    const res = await request.get("/api/patients/idcard/parse", {
      params: { card: idCard },
    });
    if (res.birthday) {
      patientForm.birthday = new Date(res.birthday);
    }
    if (res.gender) {
      patientForm.gender = res.gender;
    }
    ElMessage.success("已识别生日: " + res.birthday + "，性别: " + res.gender);
  } catch {
    // 拦截器已弹出后端错误信息
  } finally {
    idCardParsing.value = false;
  }
};

const handleSearchPatient = async () => {
  if (patientLocked.value) return ElMessage.warning("请先更换患者后再查询");
  if (!searchIdCard.value) return ElMessage.warning("请输入身份证号");
  const res = await request.get("/api/patients", {
    params: { keyword: searchIdCard.value, page: 1, size: 1 },
  });
  const patient = res.records?.[0];
  if (!patient) {
    patientForm.id = null;
    patientForm.idCard = searchIdCard.value;
    patientLocked.value = false;
    return ElMessage.warning("未查询到患者档案，可直接新建档案");
  }
  applyPatient(patient);
  patientLocked.value = false;
  ElMessage.success("档案读取成功，请确认当前患者");
};

const savePatient = async () => {
  if (!patientForm.name || !patientForm.idCard)
    return ElMessage.warning("请填写基础必填信息");
  const payload = {
    name: patientForm.name,
    idCard: patientForm.idCard,
    gender: patientForm.gender,
    birthday:
      patientForm.birthday instanceof Date
        ? patientForm.birthday.toISOString().slice(0, 10)
        : patientForm.birthday,
    phone: patientForm.phone,
  };
  const saved = patientForm.id
    ? await request.put(`/api/patients/${patientForm.id}`, payload)
    : await request.post("/api/patients", payload);
  applyPatient(saved);
  patientLocked.value = false;
  ElMessage.success("患者档案保存成功，请确认当前患者");
  return saved;
};

const confirmPatient = async () => {
  if (!patientForm.id) {
    const saved = await savePatient();
    if (!saved?.id) {
      return ElMessage.warning("患者档案尚未生成，请检查保存结果");
    }
  }
  patientLocked.value = true;
  ElMessage.success("当前患者已确认，信息已锁定");
};

const unlockPatient = () => {
  patientLocked.value = false;
  rechargeVisible.value = false;
  balanceRefundVisible.value = false;
  ElMessage.info("已解除当前患者锁定");
};

const openRechargeDialog = () => {
  if (!patientLocked.value || !patientForm.id) {
    return ElMessage.warning("请先确认当前患者");
  }
  rechargeForm.amount = 100;
  rechargeVisible.value = true;
};

const submitRecharge = async () => {
  if (!patientLocked.value || !patientForm.id) {
    return ElMessage.warning("请先确认当前患者");
  }
  if (!rechargeForm.amount || rechargeForm.amount <= 0) {
    return ElMessage.warning("充值金额必须大于0");
  }
  rechargeLoading.value = true;
  try {
    const patient = await request.post(`/api/patients/${patientForm.id}/recharge`, null, {
      params: { amount: rechargeForm.amount },
    });
    applyPatient(patient);
    patientLocked.value = true;
    rechargeVisible.value = false;
    ElMessage.success("充值成功");
  } finally {
    rechargeLoading.value = false;
  }
};

const openBalanceRefundDialog = () => {
  if (!patientLocked.value || !patientForm.id) {
    return ElMessage.warning("请先确认当前患者");
  }
  const currentBalance = Number(patientForm.balance || 0);
  if (currentBalance <= 0) {
    return ElMessage.warning("当前患者就诊卡余额不足");
  }
  balanceRefundForm.amount = Math.min(100, currentBalance);
  balanceRefundForm.reason = "";
  balanceRefundVisible.value = true;
};

const submitBalanceRefund = async () => {
  if (!patientLocked.value || !patientForm.id) {
    return ElMessage.warning("请先确认当前患者");
  }
  if (!balanceRefundForm.amount || balanceRefundForm.amount <= 0) {
    return ElMessage.warning("退费金额必须大于0");
  }
  if (balanceRefundForm.amount > Number(patientForm.balance || 0)) {
    return ElMessage.warning("退费金额不能超过当前余额");
  }
  balanceRefundLoading.value = true;
  try {
    const patient = await request.post(
      `/api/patients/${patientForm.id}/balance/refund`,
      null,
      {
        params: {
          amount: balanceRefundForm.amount,
          reason: balanceRefundForm.reason || undefined,
        },
      },
    );
    applyPatient(patient);
    patientLocked.value = true;
    balanceRefundVisible.value = false;
    ElMessage.success("退费成功");
  } finally {
    balanceRefundLoading.value = false;
  }
};

// ===== 排班挂号逻辑 =====
const departments = ref([]);
const scheduleQuery = reactive({ department: "", doctorName: "", date: "" });
const scheduleList = ref([]);
const selectedSchedules = ref([]);
const scheduleLoading = ref(false);
let scheduleRequestId = 0;
const schedulePage = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const doctorFee = (title = "") => {
  if (title.includes("主任")) return 50;
  if (title.includes("副主任")) return 30;
  return 20;
};

const formatDate = (value) => {
  if (!value) return "";
  if (value instanceof Date) return value.toISOString().slice(0, 10);
  return value;
};

const shiftTimeRange = (shift) => {
  if (shift === "上午") return "08:00-12:00";
  if (shift === "下午") return "13:00-17:00";
  return "08:00-17:00";
};

const doctorStatusText = (row) => {
  return row.availabilityStatus || (["在岗", "在诊"].includes(row.attendanceStatus) ? "正常" : "休息");
};

const doctorStatusType = (row) => {
  return doctorStatusText(row) === "正常" ? "success" : "info";
};

const totalFee = computed(() =>
  selectedSchedules.value.reduce((sum, item) => sum + Number(item.fee || 0), 0),
);

const fetchSchedules = async (resetPage = false) => {
  if (resetPage === true) {
    schedulePage.page = 1;
  }
  const requestId = ++scheduleRequestId;
  scheduleLoading.value = true;
  try {
    const res = await request.get("/api/doctors/schedules", {
      params: {
        department: scheduleQuery.department,
        doctorName: scheduleQuery.doctorName,
        date: formatDate(scheduleQuery.date),
        availableOnly: true,
        page: schedulePage.page,
        size: schedulePage.size,
      },
    });
    if (requestId !== scheduleRequestId) return;
    scheduleList.value = (res.records || []).map((schedule) => ({
      id: schedule.id,
      doctorId: schedule.doctorId,
      scheduleDate: schedule.scheduleDate,
      doctorName: schedule.doctorName || schedule.name,
      title: schedule.title || "普通医师",
      department: schedule.department,
      shift: schedule.shift || schedule.attendanceStatus || "全天",
      visitTime: `${schedule.scheduleDate || formatDate(scheduleQuery.date) || "--"} ${schedule.shift || "全天"} ${shiftTimeRange(schedule.shift)}`,
      attendanceStatus: schedule.attendanceStatus,
      availabilityStatus: schedule.availabilityStatus,
      fee: doctorFee(schedule.title),
      remain: schedule.status === 0 || !["在岗", "在诊"].includes(schedule.attendanceStatus)
        ? 0
        : (schedule.remain ?? schedule.limit ?? 0),
    }));
    schedulePage.total = res.total || 0;
    selectedSchedules.value = [];
  } finally {
    if (requestId === scheduleRequestId) {
      scheduleLoading.value = false;
    }
  }
};

const handleSchedulePageSizeChange = (size) => {
  schedulePage.page = 1;
  schedulePage.size = size;
  fetchSchedules();
};

const isScheduleSelectable = (row) => {
  return row.remain > 0 && ["在岗", "在诊"].includes(row.attendanceStatus);
};

const handleSelectionChange = (rows) => {
  selectedSchedules.value = rows;
};

const submitRegistration = async () => {
  if (!patientLocked.value) return ElMessage.warning("请先确认当前患者");
  if (!selectedSchedules.value.length) return ElMessage.warning("请至少选择一个医生号源");
  if (Number(patientForm.balance || 0) < totalFee.value) {
    return ElMessage.warning("就诊卡余额不足，请先充值");
  }
  const regNos = [];
  let latestPatient = null;
  for (const schedule of selectedSchedules.value) {
    const reg = await request.post("/api/patients/registrations", {
      patientId: patientForm.id,
      doctorId: schedule.doctorId,
      department: schedule.department,
      scheduleDate:
        schedule.scheduleDate ||
        formatDate(scheduleQuery.date) ||
        new Date().toISOString().slice(0, 10),
      shift: schedule.shift,
      fee: schedule.fee,
    });
    latestPatient = await request.post(`/api/patients/registrations/${reg.id}/pay`);
    regNos.push(`REG${reg.id}`);
  }
  if (latestPatient) {
    applyPatient(latestPatient);
    patientLocked.value = true;
  }
  ElMessage.success(`挂号成功！挂号单号：${regNos.join("、")}`);
  fetchSchedules();
};

onMounted(async () => {
  departments.value = await fetchDepartmentOptions();
  if (!scheduleQuery.department) {
    scheduleQuery.department = departments.value[0] || "";
  }
  fetchSchedules();
});
</script>

<style scoped>
.registration-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.info-card,
.schedule-card {
  border: none;
  min-height: 700px;
  border-radius: 4px;
}
.card-header .title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.search-bar {
  margin-bottom: 25px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.patient-form {
  padding: 0 10px;
}
.balance-text {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}
.form-actions {
  margin-top: 30px;
}

.schedule-filter {
  background-color: #fafafa;
  padding: 15px 15px 0;
  margin-bottom: 15px;
  border-radius: 4px;
}
.fee-text {
  color: #f56c6c;
  font-weight: bold;
}

.bottom-action {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.bottom-action .summary {
  font-size: 14px;
  color: #606266;
}
.bottom-action .highlight {
  font-weight: bold;
  font-size: 16px;
  margin: 0 10px;
}
</style>
