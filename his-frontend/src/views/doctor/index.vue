<!-- src/views/doctor/index.vue -->
<template>
  <div class="doctor-workspace">
    <!-- 顶部状态栏 -->
    <div v-if="activePatient" class="status-bar">
      <el-tag type="success" effect="light" size="large" class="status-tag">
        <el-icon><SuccessFilled /></el-icon> 正在为患者
        {{ activePatient.name }} 接诊
      </el-tag>
    </div>

    <el-row :gutter="20">
      <!-- 左侧患者队列 -->
      <el-col :span="6">
        <el-card shadow="never" class="queue-card">
          <div class="queue-header">
            <span class="title">今日患者队列</span>
            <span class="wait-count">{{ waitingQueue.length }} 人待诊</span>
          </div>

          <el-tabs v-model="queueTab" class="queue-tabs">
            <el-tab-pane label="待诊患者" name="waiting">
              <div
                v-for="p in waitingQueue"
                :key="p.id"
                class="patient-card"
                :class="{ active: activePatient?.id === p.id }"
              >
                <div class="p-info">
                  <div class="p-name">{{ p.name }}</div>
                  <div class="p-meta">
                    {{ p.department || "--" }} | {{ p.scheduleDate || "--" }}
                  </div>
                </div>
                <el-button
                  type="primary"
                  plain
                  size="small"
                  @click="callPatient(p)"
                  >接诊</el-button
                >
              </div>
              <el-empty
                v-if="waitingQueue.length === 0"
                description="暂无待诊患者"
                :image-size="60"
              />
            </el-tab-pane>
            <el-tab-pane label="已诊患者" name="seen">
              <el-empty description="暂无已诊患者" :image-size="60" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <!-- 右侧接诊区 -->
      <el-col :span="18">
        <el-card shadow="never" class="workspace-card">
          <template v-if="!activePatient">
            <el-empty
              description="请从左侧队列呼叫患者接诊"
              :image-size="120"
              style="margin-top: 100px"
            />
          </template>
          <template v-else>
            <!-- 患者基础信息表格 -->
            <div class="patient-info-bar">
              <el-descriptions border :column="4" class="custom-desc">
                <el-descriptions-item
                  label="姓名"
                  label-class-name="desc-label"
                  content-class-name="desc-content"
                  >{{ activePatient.name }}</el-descriptions-item
                >
                <el-descriptions-item
                  label="性别"
                  label-class-name="desc-label"
                  content-class-name="desc-content"
                  >{{ activePatient.gender || "男" }}</el-descriptions-item
                >
                <el-descriptions-item
                  label="年龄"
                  label-class-name="desc-label"
                  content-class-name="desc-content"
                  >{{ activePatient.age || "65岁" }}</el-descriptions-item
                >
                <el-descriptions-item
                  label="挂号单号"
                  label-class-name="desc-label"
                  content-class-name="desc-content"
                  >{{ activePatient.regNo || "--" }}</el-descriptions-item
                >
              </el-descriptions>
            </div>

            <!-- 病历 / 处方区域 -->
            <el-tabs v-model="activeTab" class="content-tabs">
              <el-tab-pane label="门诊病历书写" name="record">
                <el-form
                  :model="recordForm"
                  label-position="left"
                  label-width="80px"
                  class="record-form"
                >
                  <el-form-item label="主诉">
                    <el-input
                      v-model="recordForm.chiefComplaint"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入患者主诉内容..."
                    />
                  </el-form-item>
                  <el-form-item label="现病史">
                    <el-input
                      v-model="recordForm.presentIllness"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入现病史..."
                    />
                  </el-form-item>
                  <el-form-item label="既往史">
                    <el-input
                      v-model="recordForm.pastHistory"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入既往史（如无则填无）..."
                    />
                  </el-form-item>
                  <el-form-item label="初步诊断">
                    <el-input
                      v-model="recordForm.diagnosis"
                      placeholder="请输入ICD编码或诊断名称..."
                    />
                  </el-form-item>
                </el-form>

                <div class="form-actions">
                  <el-button @click="saveDraft">暂存病历</el-button>
                  <el-button type="success" @click="submitRecord"
                    >诊毕提交</el-button
                  >
                </div>
              </el-tab-pane>

              <el-tab-pane label="处方开立 (0)" name="prescription">
                <el-empty description="暂无处方信息" :image-size="80" />
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
import { ElMessage } from "element-plus";

const queueTab = ref("waiting");
const activeTab = ref("record");
const activePatient = ref(null);
const waitingQueue = ref([]);

const recordForm = reactive({
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  diagnosis: "",
});

const fetchQueue = async () => {
  try {
    const [waiting, waitingLegacy] = await Promise.all([
      request.get("/api/patients/registrations/me", {
        params: { status: "待诊", page: 1, size: 100 },
      }),
      request.get("/api/patients/registrations/me", {
        params: { status: "待诊中", page: 1, size: 100 },
      }),
    ]);
    waitingQueue.value = [
      ...(waiting.records || []),
      ...(waitingLegacy.records || []),
    ].map((item) => ({
      id: item.id,
      patientId: item.patientId,
      doctorId: item.doctorId,
      name: item.patientName || `患者#${item.patientId}`,
      gender: "",
      age: "",
      regNo: item.regNo,
      department: item.department,
      scheduleDate: item.scheduleDate,
    }));
  } catch (e) {
    console.error(e);
  }
};

const callPatient = async (patient) => {
  await request.post(`/api/doctors/call/${patient.patientId}`);
  activePatient.value = patient;
  Object.keys(recordForm).forEach((key) => (recordForm[key] = ""));
};

const saveDraft = () => {
  ElMessage.success("病历已暂存");
};

const submitRecord = async () => {
  if (!recordForm.diagnosis) return ElMessage.warning("请填写初步诊断");

  const submitData = {
    patientId: activePatient.value.patientId,
    doctorId: activePatient.value.doctorId,
    diagnosis: recordForm.diagnosis,
    treatmentPlan: `主诉: ${recordForm.chiefComplaint}\n现病史: ${recordForm.presentIllness}\n既往史: ${recordForm.pastHistory}`,
  };

  try {
    await request.post("/api/doctors/records", submitData);
    ElMessage.success("诊毕提交成功");
    activePatient.value = null;
    fetchQueue();
  } catch (error) {
    console.error(error);
  }
};

onMounted(() => {
  fetchQueue();
});
</script>

<style scoped>
.doctor-workspace {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.status-bar {
  text-align: center;
  margin-bottom: 20px;
}
.status-tag {
  padding: 10px 30px;
  font-size: 15px;
  border-radius: 4px;
}

.queue-card,
.workspace-card {
  border: none;
  border-radius: 4px;
  min-height: 750px;
}
.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.queue-header .title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
.queue-header .wait-count {
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.patient-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  margin-bottom: 10px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid transparent;
}
.patient-card.active {
  background: #f0f7ff;
  border-color: #c6e2ff;
}
.p-name {
  font-weight: bold;
  font-size: 15px;
  color: #303133;
  margin-bottom: 6px;
}
.p-meta {
  font-size: 12px;
  color: #909399;
}

.patient-info-bar {
  margin-bottom: 25px;
}
:deep(.desc-label) {
  background-color: #fafafa !important;
  color: #606266;
  width: 80px;
  text-align: center !important;
}
:deep(.desc-content) {
  color: #303133;
  text-align: center !important;
}

.record-form {
  margin-top: 20px;
}
.form-actions {
  text-align: right;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
