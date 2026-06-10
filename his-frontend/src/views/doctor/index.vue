<template>
  <div class="doctor-workspace">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never" class="queue-card">
          <template #header>
            <div
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
              "
            >
              <span style="font-weight: bold"
                ><el-icon><List /></el-icon> 门诊排班队列</span
              >
              <el-button
                type="primary"
                link
                icon="Refresh"
                @click="fetchSchedules"
                >刷新</el-button
              >
            </div>
          </template>

          <el-table
            :data="queueData"
            v-loading="loading"
            highlight-current-row
            @current-change="handlePatientSelect"
            style="width: 100%; cursor: pointer"
          >
            <el-table-column prop="id" label="医生ID" width="80" />
            <el-table-column prop="name" label="出诊医生" />
            <el-table-column prop="department" label="科室" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" style="min-height: 500px">
          <template v-if="!activeQueue">
            <el-empty
              description="请先在左侧队列中选择医生排班或呼叫患者"
              :image-size="120"
              style="margin-top: 80px"
            />
          </template>

          <template v-else>
            <div
              style="
                margin-bottom: 20px;
                padding: 10px;
                background: #f4f4f5;
                border-radius: 4px;
              "
            >
              <span style="margin-right: 20px"
                >当前排班：<strong
                  >{{ activeQueue.name }} ({{ activeQueue.department }})</strong
                ></span
              >
              <el-button type="success" size="small" plain @click="callPatient"
                >系统语音叫号</el-button
              >
            </div>

            <el-tabs v-model="activeTab" type="border-card">
              <el-tab-pane label="书写电子病历" name="record">
                <el-form
                  :model="recordForm"
                  label-width="80px"
                  label-position="top"
                >
                  <el-form-item label="关联患者ID (手动输入以模拟就诊)">
                    <el-input-number
                      v-model="recordForm.patientId"
                      :min="1"
                      style="width: 200px"
                    />
                  </el-form-item>
                  <el-form-item label="主诉与现病史">
                    <el-input
                      v-model="recordForm.diagnosis"
                      type="textarea"
                      :rows="4"
                      placeholder="请详细描述患者临床诊断结果..."
                    />
                  </el-form-item>
                  <el-form-item label="治疗方案与医嘱">
                    <el-input
                      v-model="recordForm.treatmentPlan"
                      type="textarea"
                      :rows="4"
                      placeholder="请输入治疗建议或手术方案..."
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      icon="DocumentChecked"
                      @click="submitRecord"
                      >病历签名并归档</el-button
                    >
                  </el-form-item>
                </el-form>
              </el-tab-pane>

              <el-tab-pane label="开具医药处方" name="prescription">
                <el-form
                  :model="prescriptionForm"
                  label-width="80px"
                  label-position="top"
                >
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="处方类型">
                        <el-select
                          v-model="prescriptionForm.type"
                          style="width: 100%"
                        >
                          <el-option label="西药处方" value="WESTERN" />
                          <el-option label="中成药处方" value="CHINESE" />
                          <el-option label="医技检查单" value="CHECKUP" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="预估总金额 (元)">
                        <el-input-number
                          v-model="prescriptionForm.totalAmount"
                          :min="0"
                          :precision="2"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="RP. (药品/项目明细)">
                    <el-input
                      v-model="prescriptionForm.drugItems"
                      type="textarea"
                      :rows="5"
                      placeholder="例如：阿莫西林胶囊 0.5g x 24粒，1次/日..."
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="success"
                      icon="Check"
                      @click="submitPrescription"
                      >下发处方至药房/收费处</el-button
                    >
                  </el-form-item>
                </el-form>
              </el-tab-pane>

              <el-tab-pane label="患者历史病历" name="history">
                <div style="display: flex; gap: 10px; margin-bottom: 20px">
                  <el-input-number
                    v-model="searchPatientId"
                    :min="1"
                    placeholder="患者ID"
                    style="width: 150px"
                  />
                  <el-button type="primary" @click="fetchHistory"
                    >查询病史</el-button
                  >
                </div>
                <el-timeline v-if="historyList.length > 0">
                  <el-timeline-item
                    v-for="h in historyList"
                    :key="h.id"
                    :timestamp="h.createdAt"
                    placement="top"
                    type="primary"
                  >
                    <el-card>
                      <h4>诊断：{{ h.diagnosis }}</h4>
                      <p style="color: #666; margin-top: 10px">
                        方案：{{ h.treatmentPlan }}
                      </p>
                    </el-card>
                  </el-timeline-item>
                </el-timeline>
                <el-empty
                  v-else
                  description="暂无历史诊疗记录"
                  :image-size="80"
                />
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage, ElNotification } from "element-plus";
import { useUserStore } from "../../store/user";

const userStore = useUserStore();
const loading = ref(false);
const queueData = ref([]);
const activeQueue = ref(null);
const activeTab = ref("record");

const searchPatientId = ref(1);
const historyList = ref([]);

const recordForm = reactive({
  patientId: 1,
  doctorId: userStore.userId || 1,
  diagnosis: "",
  treatmentPlan: "",
});
const prescriptionForm = reactive({
  patientId: 1,
  doctorId: userStore.userId || 1,
  type: "WESTERN",
  drugItems: "",
  totalAmount: 0,
});

const fetchSchedules = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/doctors/schedules", {
      params: { page: 1, size: 50 },
    });
    queueData.value = res.records || [];
  } finally {
    loading.value = false;
  }
};

const handlePatientSelect = (row) => {
  activeQueue.value = row;
  // 将表单的医生ID自动绑定为选中的排班医生
  if (row) {
    recordForm.doctorId = row.id;
    prescriptionForm.doctorId = row.id;
  }
};

const callPatient = async () => {
  if (!recordForm.patientId)
    return ElMessage.warning("请先指定需要呼叫的患者ID");
  const msg = await request.post(`/api/doctors/call/${recordForm.patientId}`);
  ElNotification({
    title: "门诊播报",
    message: msg || `请 ${recordForm.patientId} 号患者就诊`,
    type: "success",
  });
};

const submitRecord = async () => {
  if (!recordForm.diagnosis) return ElMessage.warning("临床诊断不允许为空");
  await request.post("/api/doctors/records", recordForm);
  ElMessage.success("电子病历已成功归档入库！");
  recordForm.diagnosis = "";
  recordForm.treatmentPlan = "";
};

const submitPrescription = async () => {
  if (!prescriptionForm.drugItems) return ElMessage.warning("请输入药品明细");
  await request.post("/api/doctors/prescriptions", prescriptionForm);
  ElMessage.success("处方开具成功，已同步至计费系统！");
  prescriptionForm.drugItems = "";
  prescriptionForm.totalAmount = 0;
};

const fetchHistory = async () => {
  if (!searchPatientId.value) return;
  const res = await request.get(
    `/api/doctors/records/history/${searchPatientId.value}`,
  );
  historyList.value = res || [];
};

onMounted(() => {
  fetchSchedules();
});
</script>

<style scoped>
.doctor-workspace {
  padding: 10px;
}
.queue-card {
  min-height: 500px;
  border-right: 2px solid #ebeef5;
}
</style>
