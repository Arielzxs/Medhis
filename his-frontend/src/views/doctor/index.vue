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
          <el-input
            v-model.trim="queueKeyword"
            class="queue-search"
            placeholder="查询姓名/挂号单/患者ID"
            clearable
          />

          <el-tabs v-model="queueTab" class="queue-tabs">
            <el-tab-pane label="待诊患者" name="waiting">
              <div
                v-for="p in pagedWaitingQueue"
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
                v-if="filteredWaitingQueue.length === 0"
                description="暂无待诊患者"
                :image-size="60"
              />
              <div v-if="filteredWaitingQueue.length > waitingPage.size" class="queue-pagination">
                <el-pagination
                  v-model:current-page="waitingPage.page"
                  small
                  background
                  layout="total, prev, pager, next"
                  :page-size="waitingPage.size"
                  :total="filteredWaitingQueue.length"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane label="已诊患者" name="seen">
              <div v-for="p in pagedSeenQueue" :key="p.id" class="patient-card seen-card">
                <div class="p-info">
                  <div class="p-name">{{ p.name }}</div>
                  <div class="p-meta">
                    {{ p.department || "--" }} | {{ p.scheduleDate || "--" }}
                  </div>
                </div>
                <el-tag type="success" effect="light" size="small">{{ p.status || "已完成" }}</el-tag>
              </div>
              <el-empty
                v-if="filteredSeenQueue.length === 0"
                description="暂无已诊患者"
                :image-size="60"
              />
              <div v-if="filteredSeenQueue.length > seenPage.size" class="queue-pagination">
                <el-pagination
                  v-model:current-page="seenPage.page"
                  small
                  background
                  layout="total, prev, pager, next"
                  :page-size="seenPage.size"
                  :total="filteredSeenQueue.length"
                />
              </div>
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

              <el-tab-pane :label="`处方开立 (${prescriptionItems.length})`" name="prescription">
                <div class="prescription-tools">
                  <el-input
                    v-model.trim="drugQuery.keyword"
                    placeholder="输入药品名称或编码搜索"
                    clearable
                    @keyup.enter="searchDrugs"
                  />
                  <el-button type="primary" :loading="loadingDrugs" @click="searchDrugs">搜索药品</el-button>
                </div>

                <el-table
                  :data="drugResults"
                  border
                  stripe
                  size="small"
                  class="drug-result-table"
                  v-loading="loadingDrugs"
                >
                  <el-table-column prop="code" label="编码" align="center" width="110" />
                  <el-table-column prop="name" label="药品名称" min-width="140" />
                  <el-table-column prop="unit" label="单位" align="center" width="70" />
                  <el-table-column label="单价" align="center" width="90">
                    <template #default="{ row }">¥ {{ Number(row.price || 0).toFixed(2) }}</template>
                  </el-table-column>
                  <el-table-column prop="stock" label="库存" align="center" width="80">
                    <template #default="{ row }">
                      <el-tag :type="row.stock > 0 ? 'success' : 'danger'" effect="light">
                        {{ row.stock || 0 }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" align="center" width="90">
                    <template #default="{ row }">
                      <el-button
                        type="primary"
                        link
                        size="small"
                        :disabled="!row.stock"
                        @click="addDrugToPrescription(row)"
                        >加入</el-button
                      >
                    </template>
                  </el-table-column>
                </el-table>

                <div class="prescription-title">本次处方</div>
                <el-table
                  :data="prescriptionItems"
                  border
                  size="small"
                  empty-text="请先搜索并加入药品"
                >
                  <el-table-column prop="drugName" label="药品名称" min-width="140" />
                  <el-table-column label="库存" align="center" width="80">
                    <template #default="{ row }">{{ row.stock }}</template>
                  </el-table-column>
                  <el-table-column label="数量" align="center" width="130">
                    <template #default="{ row }">
                      <el-input-number
                        v-model="row.quantity"
                        :min="1"
                        :max="row.stock"
                        size="small"
                        style="width: 104px"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="小计" align="center" width="100">
                    <template #default="{ row }">
                      ¥ {{ (Number(row.price || 0) * row.quantity).toFixed(2) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" align="center" width="80">
                    <template #default="{ $index }">
                      <el-button type="danger" link size="small" @click="removePrescriptionItem($index)">移除</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <div class="prescription-actions">
                  <span class="amount-text">合计：¥ {{ prescriptionTotal.toFixed(2) }}</span>
                  <el-button
                    type="success"
                    :loading="submittingPrescription"
                    :disabled="!prescriptionItems.length"
                    @click="submitPrescription"
                    >提交处方</el-button
                  >
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import request from "../../utils/request";
import { ElMessage } from "element-plus";

const queueTab = ref("waiting");
const activeTab = ref("record");
const activePatient = ref(null);
const queueKeyword = ref("");
const waitingQueue = ref([]);
const seenQueue = ref([]);
const waitingPage = reactive({ page: 1, size: 5 });
const seenPage = reactive({ page: 1, size: 5 });
const loadingDrugs = ref(false);
const submittingPrescription = ref(false);
const drugResults = ref([]);
const prescriptionItems = ref([]);
const drugQuery = reactive({ keyword: "" });

const patientMatched = (patient) => {
  const keyword = queueKeyword.value.trim();
  if (!keyword) return true;
  return [patient.name, patient.regNo, patient.patientId, patient.department, patient.scheduleDate]
    .filter((item) => item !== undefined && item !== null)
    .some((item) => String(item).includes(keyword));
};

const filteredWaitingQueue = computed(() => waitingQueue.value.filter(patientMatched));
const filteredSeenQueue = computed(() => seenQueue.value.filter(patientMatched));

const pagedWaitingQueue = computed(() => {
  const start = (waitingPage.page - 1) * waitingPage.size;
  return filteredWaitingQueue.value.slice(start, start + waitingPage.size);
});

const pagedSeenQueue = computed(() => {
  const start = (seenPage.page - 1) * seenPage.size;
  return filteredSeenQueue.value.slice(start, start + seenPage.size);
});

const prescriptionTotal = computed(() =>
  prescriptionItems.value.reduce(
    (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0),
    0,
  ),
);

watch(queueKeyword, () => {
  waitingPage.page = 1;
  seenPage.page = 1;
});

const recordForm = reactive({
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  diagnosis: "",
});

const emptyRecordForm = () => ({
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  diagnosis: "",
});

const draftKey = (patient = activePatient.value) => {
  if (!patient) return "";
  return `doctor-record-draft:${patient.id || patient.regNo || patient.patientId}`;
};

const clearRecordForm = () => {
  Object.assign(recordForm, emptyRecordForm());
};

const loadDraft = (patient) => {
  clearRecordForm();
  const key = draftKey(patient);
  if (!key) return;
  const draftText = localStorage.getItem(key);
  if (!draftText) return;
  try {
    Object.assign(recordForm, {
      ...emptyRecordForm(),
      ...JSON.parse(draftText),
    });
    ElMessage.success("已恢复上次暂存的病历");
  } catch {
    localStorage.removeItem(key);
  }
};

const mapRegistrationToPatient = (item) => ({
  id: item.id,
  patientId: item.patientId,
  doctorId: item.doctorId,
  name: item.patientName || `患者#${item.patientId}`,
  gender: "",
  age: "",
  regNo: item.regNo,
  department: item.department,
  scheduleDate: item.scheduleDate,
  status: item.status,
  createdAt: item.createdAt || "",
});

const fetchRegistrationsByStatus = async (doctorId, status) => {
  const firstPage = await request.get("/api/patients/registrations", {
    params: { doctorId, status, page: 1, size: 50 },
  });
  const pageSize = firstPage.size || 50;
  const totalPages = Math.max(Math.ceil((firstPage.total || 0) / pageSize), 1);
  const restPages = await Promise.all(
    Array.from({ length: totalPages - 1 }, (_, index) =>
      request.get("/api/patients/registrations", {
        params: { doctorId, status, page: index + 2, size: pageSize },
      }),
    ),
  );
  return [
    ...(firstPage.records || []),
    ...restPages.flatMap((pageData) => pageData.records || []),
  ];
};

const fetchRegistrationsByStatuses = async (doctorId, statuses) => {
  const groups = await Promise.all(
    statuses.map((status) => fetchRegistrationsByStatus(doctorId, status)),
  );
  return groups
    .flat()
    .map(mapRegistrationToPatient)
    .sort((a, b) => String(a.createdAt).localeCompare(String(b.createdAt)));
};

const fetchQueue = async () => {
  try {
    const profile = await request.get("/api/doctors/me/profile");
    if (!profile?.id) {
      ElMessage.warning("当前账号未关联医生档案，无法加载候诊队列");
      waitingQueue.value = [];
      seenQueue.value = [];
      return;
    }
    const [waitingRows, seenRows] = await Promise.all([
      fetchRegistrationsByStatuses(profile.id, ["待诊", "待诊中", "就诊中"]),
      fetchRegistrationsByStatuses(profile.id, ["已完成", "已诊"]),
    ]);
    waitingQueue.value = waitingRows;
    seenQueue.value = seenRows;
    waitingPage.page = 1;
    seenPage.page = 1;
  } catch (e) {
    console.error(e);
  }
};

const callPatient = async (patient) => {
  await request.post(`/api/doctors/call/${patient.patientId}`);
  activePatient.value = patient;
  prescriptionItems.value = [];
  drugResults.value = [];
  loadDraft(patient);
};

const saveDraft = () => {
  if (!activePatient.value) {
    return ElMessage.warning("请先选择接诊患者");
  }
  localStorage.setItem(
    draftKey(),
    JSON.stringify({
      chiefComplaint: recordForm.chiefComplaint,
      presentIllness: recordForm.presentIllness,
      pastHistory: recordForm.pastHistory,
      diagnosis: recordForm.diagnosis,
    }),
  );
  ElMessage.success("病历已暂存");
};

const searchDrugs = async () => {
  const keyword = drugQuery.keyword.trim();
  loadingDrugs.value = true;
  try {
    const params = { page: 1, size: 10 };
    if (keyword) {
      if (/^[A-Za-z0-9_-]+$/.test(keyword)) {
        params.codeKeyword = keyword;
      } else {
        params.nameKeyword = keyword;
      }
    }
    const res = await request.get("/api/doctors/drugs", { params });
    drugResults.value = res.records || [];
  } finally {
    loadingDrugs.value = false;
  }
};

const addDrugToPrescription = (drug) => {
  const stock = Number(drug.stock || 0);
  if (stock <= 0) {
    return ElMessage.warning("该药品暂无库存");
  }
  const existing = prescriptionItems.value.find((item) => item.drugId === drug.id);
  if (existing) {
    if (existing.quantity >= stock) {
      return ElMessage.warning("数量不能超过当前库存");
    }
    existing.quantity += 1;
    return;
  }
  prescriptionItems.value.push({
    drugId: drug.id,
    drugName: drug.name,
    drugCode: drug.code,
    unit: drug.unit,
    price: Number(drug.price || 0),
    stock,
    quantity: 1,
  });
};

const removePrescriptionItem = (index) => {
  prescriptionItems.value.splice(index, 1);
};

const submitPrescription = async () => {
  if (!activePatient.value) return ElMessage.warning("请先选择接诊患者");
  if (!prescriptionItems.value.length) return ElMessage.warning("请先加入药品");
  const invalidItem = prescriptionItems.value.find(
    (item) => item.quantity <= 0 || item.quantity > item.stock,
  );
  if (invalidItem) {
    return ElMessage.warning(`${invalidItem.drugName} 数量不能超过库存`);
  }
  submittingPrescription.value = true;
  try {
    await request.post("/api/doctors/prescriptions", {
      patientId: activePatient.value.patientId,
      doctorId: activePatient.value.doctorId,
      type: "DRUG",
      drugItems: JSON.stringify(
        prescriptionItems.value.map((item) => ({
          drugId: item.drugId,
          drugCode: item.drugCode,
          drugName: item.drugName,
          unit: item.unit,
          price: item.price,
          quantity: item.quantity,
        })),
      ),
      totalAmount: prescriptionTotal.value,
    });
    ElMessage.success("处方已提交，待药房审核");
    prescriptionItems.value = [];
    await searchDrugs();
  } finally {
    submittingPrescription.value = false;
  }
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
    localStorage.removeItem(draftKey());
    ElMessage.success("诊毕提交成功");
    activePatient.value = null;
    clearRecordForm();
    prescriptionItems.value = [];
    drugResults.value = [];
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
.queue-search {
  margin-bottom: 12px;
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
.seen-card {
  background: #f7fbf5;
  border-color: #e1f3d8;
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
.queue-pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.queue-pagination :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
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

.prescription-tools {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.drug-result-table {
  margin-bottom: 16px;
}

.prescription-title {
  margin: 10px 0;
  font-weight: 700;
  color: #303133;
}

.prescription-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.amount-text {
  color: #f56c6c;
  font-weight: 700;
}
</style>
