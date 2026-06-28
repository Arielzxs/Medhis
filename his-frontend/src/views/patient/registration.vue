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
              <el-input v-model="patientForm.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input
                v-model="patientForm.idCard"
                placeholder="请输入身份证号"
              />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="patientForm.gender">
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
              />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input
                v-model="patientForm.phone"
                placeholder="请输入手机号"
              />
            </el-form-item>
            <el-divider border-style="dashed" />
            <el-form-item label="就诊卡余额">
              <span class="balance-text"
                >¥ {{ patientForm.balance.toFixed(2) }}</span
              >
              <el-button type="primary" link style="margin-left: 15px"
                >充值</el-button
              >
            </el-form-item>

            <div class="form-actions">
              <el-button
                type="primary"
                plain
                style="width: 100%"
                @click="savePatient"
                >保存 / 更新档案</el-button
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
                <el-option label="心血管内科" value="心血管内科" />
                <el-option label="儿科" value="儿科" />
                <el-option label="消化内科" value="消化内科" />
                <el-option label="普外科" value="普外科" />
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
              <el-button type="primary" @click="fetchSchedules"
                >查询排班</el-button
              >
            </el-form-item>
          </el-form>

          <el-table
            :data="scheduleList"
            border
            stripe
            highlight-current-row
            @current-change="handleSelectSchedule"
            style="width: 100%"
            height="450"
          >
            <el-table-column width="50" align="center">
              <template #default="scope">
                <el-radio :label="scope.row.id" v-model="selectedScheduleId"
                  ><i></i
                ></el-radio>
              </template>
            </el-table-column>
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
              prop="shift"
              label="班次"
              align="center"
              width="80"
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
            />
          </div>

          <div class="bottom-action">
            <div class="summary">
              已选看诊医生：<span class="highlight">{{
                selectedSchedule?.doctorName || "--"
              }}</span>
              需支付金额：<span class="highlight fee-text"
                >¥ {{ selectedSchedule?.fee || "0.00" }}</span
              >
            </div>
            <el-button
              type="success"
              size="large"
              :disabled="!selectedSchedule || !patientForm.id"
              @click="submitRegistration"
            >
              确认挂号并缴费
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

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

const handleSearchPatient = async () => {
  if (!searchIdCard.value) return ElMessage.warning("请输入身份证号");
  const res = await request.get("/api/patients", {
    params: { keyword: searchIdCard.value, page: 1, size: 1 },
  });
  const patient = res.records?.[0];
  if (!patient) {
    patientForm.id = null;
    patientForm.idCard = searchIdCard.value;
    return ElMessage.warning("未查询到患者档案，可直接新建档案");
  }
  Object.assign(patientForm, {
    id: patient.id,
    name: patient.name || "",
    idCard: patient.idCard || "",
    gender: patient.gender || "未知",
    birthday: patient.birthday || "",
    phone: patient.phone || "",
    balance: 0,
  });
  ElMessage.success("档案读取成功");
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
  patientForm.id = saved.id;
  ElMessage.success("患者档案保存成功");
};

// ===== 排班挂号逻辑 =====
const scheduleQuery = reactive({ department: "", doctorName: "", date: "" });
const scheduleList = ref([]);
const selectedScheduleId = ref(null);
const selectedSchedule = ref(null);
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

const fetchSchedules = async () => {
  const res = await request.get("/api/doctors/schedules", {
    params: {
      department: scheduleQuery.department,
      doctorName: scheduleQuery.doctorName,
      date: scheduleQuery.date,
      page: schedulePage.page,
      size: schedulePage.size,
    },
  });
  scheduleList.value = (res.records || [])
    .map((schedule) => ({
      id: schedule.id,
      doctorId: schedule.doctorId,
      scheduleDate: schedule.scheduleDate,
      doctorName: schedule.doctorName || schedule.name,
      title: schedule.title || "普通医师",
      department: schedule.department,
      shift: schedule.shift || schedule.attendanceStatus || "全天",
      fee: doctorFee(schedule.title),
      remain: schedule.status === 0 ? 0 : (schedule.remain ?? schedule.limit ?? 0),
    }));
  schedulePage.total = res.total || 0;
  selectedScheduleId.value = null;
  selectedSchedule.value = null;
};

const handleSchedulePageSizeChange = () => {
  schedulePage.page = 1;
  fetchSchedules();
};

const handleSelectSchedule = (row) => {
  if (row && row.remain > 0) {
    selectedScheduleId.value = row.id;
    selectedSchedule.value = row;
  } else if (row && row.remain <= 0) {
    ElMessage.warning("该医生号源已满");
  }
};

const submitRegistration = async () => {
  if (!patientForm.id) return ElMessage.warning("请先读取或保存患者档案");
  const reg = await request.post("/api/patients/registrations", {
    patientId: patientForm.id,
    doctorId: selectedSchedule.value.doctorId,
    department: selectedSchedule.value.department,
    scheduleDate: selectedSchedule.value.scheduleDate || scheduleQuery.date || new Date().toISOString().slice(0, 10),
    fee: selectedSchedule.value.fee,
  });
  await request.post(`/api/patients/registrations/${reg.id}/pay`);
  ElMessage.success(`挂号成功！挂号单号：REG${reg.id}`);
  fetchSchedules();
};

onMounted(() => {
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
