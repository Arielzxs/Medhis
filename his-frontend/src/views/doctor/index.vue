<template>
  <div>
    <el-card>
      <template #header>
        <div
          style="
            display: flex;
            justify-content: space-between;
            align-items: center;
          "
        >
          <span>今日就诊列表</span>
          <el-button type="primary" icon="Refresh" @click="fetchSchedules"
            >刷新列表</el-button
          >
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="排班ID" width="100" />
        <el-table-column prop="name" label="医生姓名" width="120" />
        <el-table-column prop="department" label="科室" width="120" />
        <el-table-column prop="specialty" label="专长" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              plain
              @click="callPatient(scope.row.id)"
              >叫号</el-button
            >
            <el-button
              size="small"
              type="warning"
              @click="openRecordDialog(scope.row)"
              >写病历</el-button
            >
            <el-button
              size="small"
              type="success"
              @click="openPrescribeDialog(scope.row)"
              >开处方</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="recordVisible" title="书写病历" width="600px">
      <el-form :model="recordForm" label-width="80px">
        <el-form-item label="患者ID">
          <el-input-number
            v-model="recordForm.patientId"
            :min="1"
            style="width: 100%"
            placeholder="实际应从排班行或扫码获取"
          />
        </el-form-item>
        <el-form-item label="临床诊断">
          <el-input
            v-model="recordForm.diagnosis"
            type="textarea"
            :rows="3"
            placeholder="请输入诊断结果"
          />
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input
            v-model="recordForm.treatmentPlan"
            type="textarea"
            :rows="3"
            placeholder="请输入治疗建议或方案"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecord">病历归档</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="开具处方" width="600px">
      <el-form :model="prescriptionForm" label-width="100px">
        <el-form-item label="处方类型">
          <el-select v-model="prescriptionForm.type" style="width: 100%">
            <el-option label="西药" value="WESTERN" />
            <el-option label="中成药" value="CHINESE" />
            <el-option label="检查" value="CHECKUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="药品/项目">
          <el-input
            v-model="prescriptionForm.drugItems"
            type="textarea"
            :rows="3"
            placeholder="请输入药品名称、规格、用法等"
          />
        </el-form-item>
        <el-form-item label="总金额 (元)">
          <el-input-number
            v-model="prescriptionForm.totalAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPrescription"
          >提交处方</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage, ElNotification } from "element-plus";
import { useUserStore } from "../../store/user";

const userStore = useUserStore();
const loading = ref(false);
const tableData = ref([]);

const dialogVisible = ref(false);
const recordVisible = ref(false);

const prescriptionForm = reactive({
  patientId: 1,
  doctorId: userStore.userId || 1,
  type: "WESTERN",
  drugItems: "",
  totalAmount: 0,
});

const recordForm = reactive({
  patientId: 1,
  doctorId: userStore.userId || 1,
  diagnosis: "",
  treatmentPlan: "",
});

const fetchSchedules = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/doctors/schedules", {
      params: { page: 1, size: 20 },
    });
    tableData.value = res.records || [];
  } finally {
    loading.value = false;
  }
};

const callPatient = async (patientId) => {
  const msg = await request.post(`/api/doctors/call/${patientId}`);
  ElNotification({
    title: "系统广播",
    message: msg || "请患者就诊",
    type: "success",
  });
};

const openRecordDialog = (row) => {
  recordForm.diagnosis = "";
  recordForm.treatmentPlan = "";
  recordVisible.value = true;
};

const submitRecord = async () => {
  if (!recordForm.diagnosis) {
    return ElMessage.warning("临床诊断不能为空");
  }
  await request.post("/api/doctors/records", recordForm);
  ElMessage.success("病历已成功归档入库！");
  recordVisible.value = false;
};

const openPrescribeDialog = (row) => {
  prescriptionForm.drugItems = "";
  prescriptionForm.totalAmount = 0;
  dialogVisible.value = true;
};

const submitPrescription = async () => {
  if (!prescriptionForm.drugItems) {
    return ElMessage.warning("请输入药品或检查项目");
  }
  await request.post("/api/doctors/prescriptions", prescriptionForm);
  ElMessage.success("处方开具成功！");
  dialogVisible.value = false;
};

onMounted(() => {
  fetchSchedules();
});
</script>
