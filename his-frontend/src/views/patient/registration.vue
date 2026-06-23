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
              :disabled="!selectedSchedule || !patientForm.idCard"
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

// ===== 患者信息逻辑 =====
const searchIdCard = ref("");
const patientForm = reactive({
  name: "",
  idCard: "",
  gender: "男",
  birthday: "",
  phone: "",
  balance: 0.0,
});

const handleSearchPatient = () => {
  if (!searchIdCard.value) return ElMessage.warning("请输入身份证号");
  // 模拟查出患者
  patientForm.name = "李建国";
  patientForm.idCard = searchIdCard.value;
  patientForm.gender = "男";
  patientForm.phone = "13800138000";
  patientForm.balance = 150.0;
  ElMessage.success("档案读取成功");
};

const savePatient = () => {
  if (!patientForm.name || !patientForm.idCard)
    return ElMessage.warning("请填写基础必填信息");
  ElMessage.success("患者档案保存成功");
};

// ===== 排班挂号逻辑 =====
const scheduleQuery = reactive({ department: "", doctorName: "", date: "" });
const scheduleList = ref([]);
const selectedScheduleId = ref(null);
const selectedSchedule = ref(null);

const fetchSchedules = () => {
  // 模拟假数据
  scheduleList.value = [
    {
      id: 1,
      doctorName: "王鹏",
      title: "主任医师",
      department: "心血管内科",
      shift: "上午",
      fee: 50.0,
      remain: 12,
    },
    {
      id: 2,
      doctorName: "孙芳",
      title: "副主任医师",
      department: "儿科",
      shift: "下午",
      fee: 30.0,
      remain: 5,
    },
    {
      id: 3,
      doctorName: "李静",
      title: "主治医师",
      department: "消化内科",
      shift: "上午",
      fee: 20.0,
      remain: 0,
    },
  ];
  selectedScheduleId.value = null;
  selectedSchedule.value = null;
};

const handleSelectSchedule = (row) => {
  if (row && row.remain > 0) {
    selectedScheduleId.value = row.id;
    selectedSchedule.value = row;
  } else if (row && row.remain <= 0) {
    ElMessage.warning("该医生号源已满");
  }
};

const submitRegistration = () => {
  if (patientForm.balance < selectedSchedule.value.fee) {
    return ElMessage.error("就诊卡余额不足，请先充值或选择其他支付方式");
  }
  ElMessage.success(
    `挂号成功！就诊号码：REG260621008，已扣除余额 ¥${selectedSchedule.value.fee}`,
  );
  patientForm.balance -= selectedSchedule.value.fee;
  fetchSchedules(); // 刷新号源
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
