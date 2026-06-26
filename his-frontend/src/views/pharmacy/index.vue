<template>
  <div class="pharmacy-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">药房管理工作站</h3>
      </div>

      <el-tabs v-model="activeTab" class="pharmacy-tabs">
        <el-tab-pane label="处方审核与发药" name="dispense">
          <div class="filter-box">
            <el-form :inline="true" :model="dispenseQuery">
              <el-form-item label="处方编号">
                <el-input
                  v-model="dispenseQuery.prescriptionNo"
                  placeholder="请输入处方编号"
                  clearable
                  style="width: 180px"
                />
              </el-form-item>
              <el-form-item label="患者姓名">
                <el-input
                  v-model="dispenseQuery.patientName"
                  placeholder="请输入患者姓名"
                  clearable
                  style="width: 140px"
                />
              </el-form-item>
              <el-form-item label="处方状态">
                <el-select
                  v-model="dispenseQuery.status"
                  placeholder="全部状态"
                  clearable
                  style="width: 140px"
                >
                  <el-option label="待审核" value="待审核" />
                  <el-option label="待发药" value="待发药" />
                  <el-option label="已发药" value="已发药" />
                  <el-option label="已退回" value="已退回" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchPrescriptions"
                  >查询处方</el-button
                >
                <el-button @click="resetDispenseQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="prescriptionList"
            border
            stripe
            style="width: 100%"
            v-loading="loadingDispense"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <el-table-column
              prop="prescriptionNo"
              label="处方编号"
              align="center"
              width="160"
            />
            <el-table-column
              prop="patientName"
              label="患者姓名"
              align="center"
              width="100"
            />
            <el-table-column
              prop="doctorName"
              label="开具医生"
              align="center"
              width="100"
            />
            <el-table-column
              prop="department"
              label="科室"
              align="center"
              width="120"
            />
            <el-table-column
              prop="amount"
              label="处方金额(元)"
              align="center"
              width="120"
            >
              <template #default="scope">
                <span class="amount-text"
                  >¥ {{ scope.row.amount.toFixed(2) }}</span
                >
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              label="状态"
              align="center"
              width="120"
            >
              <template #default="scope">
                <el-tag
                  :type="getPrescriptionStatusType(scope.row.status)"
                  effect="light"
                >
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="createTime"
              label="开具时间"
              align="center"
              width="160"
            />
            <el-table-column
              label="操作"
              align="center"
              fixed="right"
              width="180"
            >
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="viewDetails(scope.row)"
                  >查看明细</el-button
                >
                <el-button
                  v-if="scope.row.status === '待审核'"
                  type="warning"
                  link
                  size="small"
                  @click="openReviewDialog(scope.row)"
                  >审核</el-button
                >
                <el-button
                  v-if="scope.row.status === '待发药'"
                  type="success"
                  link
                  size="small"
                  @click="handleDispense(scope.row)"
                  >发药</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="药品字典与库存" name="inventory">
          <div class="filter-box">
            <el-form :inline="true" :model="inventoryQuery">
              <el-form-item label="药品编码">
                <el-input
                  v-model="inventoryQuery.drugCode"
                  placeholder="输入条码/编码"
                  clearable
                  style="width: 160px"
                />
              </el-form-item>
              <el-form-item label="药品名称">
                <el-input
                  v-model="inventoryQuery.drugName"
                  placeholder="输入名称/拼音码"
                  clearable
                  style="width: 160px"
                />
              </el-form-item>
              <el-form-item label="库存预警">
                <el-switch
                  v-model="inventoryQuery.showWarningOnly"
                  active-text="仅看预警"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchDrugs"
                  >检索药品</el-button
                >
                <el-button type="success" plain @click="handleAddDrug"
                  >新增药品</el-button
                >
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="filteredDrugList"
            border
            stripe
            style="width: 100%"
            v-loading="loadingInventory"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <el-table-column
              prop="drugCode"
              label="药品编码"
              align="center"
              width="120"
            />
            <el-table-column
              prop="drugName"
              label="药品名称"
              align="center"
              min-width="150"
            />
            <el-table-column
              prop="category"
              label="分类"
              align="center"
              width="100"
            />
            <el-table-column
              prop="specification"
              label="规格"
              align="center"
              width="120"
            />
            <el-table-column
              prop="unit"
              label="单位"
              align="center"
              width="80"
            />
            <el-table-column
              prop="price"
              label="零售价(元)"
              align="center"
              width="100"
            >
              <template #default="scope">
                ¥ {{ scope.row.price.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="stock"
              label="当前库存"
              align="center"
              width="100"
            >
              <template #default="scope">
                <span
                  :class="{
                    'text-danger font-bold':
                      scope.row.stock <= scope.row.warningThreshold,
                  }"
                >
                  {{ scope.row.stock }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              prop="warningThreshold"
              label="预警线"
              align="center"
              width="80"
            />
            <el-table-column label="状态" align="center" width="100">
              <template #default="scope">
                <el-tag
                  :type="
                    scope.row.stock > scope.row.warningThreshold
                      ? 'success'
                      : 'danger'
                  "
                >
                  {{
                    scope.row.stock > scope.row.warningThreshold
                      ? "正常"
                      : "库存不足"
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              align="center"
              width="150"
              fixed="right"
            >
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="openInboundDialog(scope.row)"
                  >入库</el-button
                >
                <el-button type="info" link size="small" @click="handleEditDrug(scope.row)"
                  >编辑</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="reviewDialogVisible" title="处方审核" width="600px">
      <div v-if="currentPrescription">
        <el-descriptions border :column="2" class="mb-4">
          <el-descriptions-item label="处方编号">{{
            currentPrescription.prescriptionNo
          }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{
            currentPrescription.patientName
          }}</el-descriptions-item>
          <el-descriptions-item label="开单医生">{{
            currentPrescription.doctorName
          }}</el-descriptions-item>
          <el-descriptions-item label="处方金额"
            ><span class="amount-text"
              >¥ {{ currentPrescription.amount.toFixed(2) }}</span
            ></el-descriptions-item
          >
        </el-descriptions>

        <h4 class="section-title">药品明细</h4>
        <el-table
          :data="currentPrescription.items"
          border
          size="small"
          style="margin-bottom: 20px"
        >
          <el-table-column prop="drugName" label="药品名称" />
          <el-table-column prop="specification" label="规格" width="100" />
          <el-table-column
            prop="quantity"
            label="数量"
            width="80"
            align="center"
          />
          <el-table-column prop="usage" label="用法用量" />
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="danger" plain @click="submitReview(false)"
            >审核驳回</el-button
          >
          <el-button type="success" @click="submitReview(true)"
            >审核通过</el-button
          >
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="inboundDialogVisible"
      title="药品采购入库"
      width="400px"
    >
      <el-form :model="inboundForm" label-width="90px">
        <el-form-item label="药品名称：">
          <span>{{ currentDrug?.drugName }}</span>
        </el-form-item>
        <el-form-item label="当前库存：">
          <span>{{ currentDrug?.stock }} {{ currentDrug?.unit }}</span>
        </el-form-item>
        <el-form-item label="入库数量：">
          <el-input-number
            v-model="inboundForm.quantity"
            :min="1"
            :step="10"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="供应商：">
          <el-select
            v-model="inboundForm.supplier"
            placeholder="请选择供应商"
            style="width: 100%"
          >
            <el-option label="国药控股股份有限公司" value="国药" />
            <el-option label="华润医药商业集团" value="华润" />
            <el-option label="九州通医药集团" value="九州通" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="inboundDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitInbound">确认入库</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="drugDialogVisible"
      :title="drugForm.id ? '编辑药品' : '新增药品'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="drugFormRef" :model="drugForm" :rules="drugRules" label-width="96px">
        <el-form-item label="药品编码" prop="code">
          <el-input v-model.trim="drugForm.code" placeholder="例如：DRUG001" />
        </el-form-item>
        <el-form-item label="药品名称" prop="name">
          <el-input v-model.trim="drugForm.name" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-select v-model="drugForm.unit" placeholder="请选择单位" filterable allow-create style="width: 100%">
            <el-option label="盒" value="盒" />
            <el-option label="瓶" value="瓶" />
            <el-option label="支" value="支" />
            <el-option label="片" value="片" />
            <el-option label="袋" value="袋" />
          </el-select>
        </el-form-item>
        <el-form-item label="零售价" prop="price">
          <el-input-number v-model="drugForm.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前库存" prop="stock">
          <el-input-number v-model="drugForm.stock" :min="0" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警线" prop="warningThreshold">
          <el-input-number v-model="drugForm.warningThreshold" :min="0" :step="10" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="drugDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="savingDrug" @click="submitDrug">保 存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../utils/request";

const activeTab = ref("dispense");

// ================= 处方审核与发药 =================
const loadingDispense = ref(false);
const dispenseQuery = reactive({
  prescriptionNo: "",
  patientName: "",
  status: "",
});
const prescriptionList = ref([]);

const reviewDialogVisible = ref(false);
const currentPrescription = ref(null);

const prescriptionStatus = (row) => {
  if (row.auditStatus === "REJECTED") return "已退回";
  if (row.dispenseStatus === "DONE") return "已发药";
  if (row.auditStatus === "APPROVED" && row.paid === "Y") return "待发药";
  return "待审核";
};

const parseItems = (text) => {
  if (!text) return [];
  try {
    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [{ drugName: text, specification: "--", quantity: 1, usage: "--" }];
  }
};

const fetchPrescriptions = async () => {
  loadingDispense.value = true;
  try {
    const res = await request.get("/api/doctors/prescriptions");
    prescriptionList.value = (res || [])
      .map((item) => ({
        id: item.id,
        prescriptionNo: `RX${item.id}`,
        patientName: `患者#${item.patientId}`,
        doctorName: `医生#${item.doctorId}`,
        department: "--",
        amount: Number(item.totalAmount || 0),
        status: prescriptionStatus(item),
        createTime: item.createdAt || "",
        items: parseItems(item.drugItems),
      }))
      .filter((item) => {
        const noMatched = dispenseQuery.prescriptionNo
          ? item.prescriptionNo.includes(dispenseQuery.prescriptionNo)
          : true;
        const nameMatched = dispenseQuery.patientName
          ? item.patientName.includes(dispenseQuery.patientName)
          : true;
        const statusMatched = dispenseQuery.status
          ? item.status === dispenseQuery.status
          : true;
        return noMatched && nameMatched && statusMatched;
      });
  } finally {
    loadingDispense.value = false;
  }
};

const getPrescriptionStatusType = (status) => {
  const map = {
    待审核: "warning",
    待发药: "primary",
    已发药: "success",
    已退回: "info",
  };
  return map[status] || "info";
};

const resetDispenseQuery = () => {
  dispenseQuery.prescriptionNo = "";
  dispenseQuery.patientName = "";
  dispenseQuery.status = "";
  fetchPrescriptions();
};

const viewDetails = (row) => {
  currentPrescription.value = row;
  reviewDialogVisible.value = true;
};

const openReviewDialog = (row) => {
  currentPrescription.value = row;
  reviewDialogVisible.value = true;
};

const submitReview = async (isPass) => {
  await request.post(
    `/api/pharmacy/prescriptions/${currentPrescription.value.id}/review`,
    null,
    { params: { approved: isPass } },
  );
  if (isPass) {
    ElMessage.success(
      `处方 ${currentPrescription.value.prescriptionNo} 审核通过，流转至待发药`,
    );
    currentPrescription.value.status = "待发药";
  } else {
    ElMessage.warning(
      `处方 ${currentPrescription.value.prescriptionNo} 已驳回`,
    );
    currentPrescription.value.status = "已退回";
  }
  reviewDialogVisible.value = false;
};

const handleDispense = (row) => {
  ElMessageBox.confirm(
    `确认对患者 ${row.patientName} 的处方进行发药并扣减库存吗？`,
    "发药确认",
    {
      confirmButtonText: "确认发药",
      cancelButtonText: "取消",
      type: "warning",
    },
  )
    .then(async () => {
      const firstItem = row.items?.[0];
      if (!firstItem?.drugId) {
        ElMessage.warning("处方明细缺少药品 ID，无法自动发药");
        return;
      }
      await request.post(`/api/pharmacy/prescriptions/${row.id}/dispense`, null, {
        params: { drugId: firstItem.drugId, quantity: firstItem.quantity || 1 },
      });
      ElMessage.success("发药成功，库存已自动扣减");
      fetchPrescriptions();
    })
    .catch(() => {});
};

// ================= 药品字典与库存 =================
const loadingInventory = ref(false);
const inventoryQuery = reactive({
  drugCode: "",
  drugName: "",
  showWarningOnly: false,
});
const drugList = ref([]);

const inboundDialogVisible = ref(false);
const drugDialogVisible = ref(false);
const savingDrug = ref(false);
const drugFormRef = ref();
const currentDrug = ref(null);
const inboundForm = reactive({ quantity: 50, supplier: "" });
const drugForm = reactive({
  id: null,
  code: "",
  name: "",
  unit: "盒",
  price: 0,
  stock: 0,
  warningThreshold: 20,
});

const drugRules = {
  code: [
    { required: true, message: "请输入药品编码", trigger: "blur" },
    { pattern: /^[A-Za-z0-9_-]{2,32}$/, message: "编码只能包含字母、数字、下划线或短横线", trigger: "blur" },
  ],
  name: [{ required: true, message: "请输入药品名称", trigger: "blur" }],
  unit: [{ required: true, message: "请选择单位", trigger: "change" }],
  price: [{ required: true, message: "请输入零售价", trigger: "blur" }],
  stock: [{ required: true, message: "请输入库存", trigger: "blur" }],
  warningThreshold: [{ required: true, message: "请输入预警线", trigger: "blur" }],
};

const fetchDrugs = async () => {
  loadingInventory.value = true;
  try {
    const res = await request.get("/api/pharmacy/inventory", {
      params: { page: 1, size: 100 },
    });
    drugList.value = (res.records || [])
      .map((item) => ({
        id: item.id,
        drugCode: item.code,
        drugName: item.name,
        category: "--",
        specification: "--",
        unit: item.unit || "",
        price: Number(item.price || 0),
        stock: item.stock || 0,
        warningThreshold: item.warningThreshold || 0,
      }))
      .filter((item) => {
        const codeMatched = inventoryQuery.drugCode
          ? item.drugCode?.includes(inventoryQuery.drugCode)
          : true;
        const nameMatched = inventoryQuery.drugName
          ? item.drugName?.includes(inventoryQuery.drugName)
          : true;
        return codeMatched && nameMatched;
      });
  } finally {
    loadingInventory.value = false;
  }
};

const filteredDrugList = computed(() => {
  if (inventoryQuery.showWarningOnly) {
    return drugList.value.filter((d) => d.stock <= d.warningThreshold);
  }
  return drugList.value;
});

const resetDrugForm = () => {
  Object.assign(drugForm, {
    id: null,
    code: "",
    name: "",
    unit: "盒",
    price: 0,
    stock: 0,
    warningThreshold: 20,
  });
};

const handleAddDrug = () => {
  resetDrugForm();
  drugDialogVisible.value = true;
};

const handleEditDrug = (row) => {
  Object.assign(drugForm, {
    id: row.id,
    code: row.drugCode,
    name: row.drugName,
    unit: row.unit || "盒",
    price: row.price || 0,
    stock: row.stock || 0,
    warningThreshold: row.warningThreshold || 0,
  });
  drugDialogVisible.value = true;
};

const submitDrug = async () => {
  await drugFormRef.value?.validate();
  savingDrug.value = true;
  try {
    const payload = {
      code: drugForm.code,
      name: drugForm.name,
      unit: drugForm.unit,
      price: drugForm.price,
      stock: drugForm.stock,
      warningThreshold: drugForm.warningThreshold,
    };
    if (drugForm.id) {
      payload.id = drugForm.id;
    }
    await request.post("/api/pharmacy/drugs", payload);
    ElMessage.success(drugForm.id ? "药品信息已更新" : "药品新增成功");
    drugDialogVisible.value = false;
    fetchDrugs();
  } finally {
    savingDrug.value = false;
  }
};

const openInboundDialog = (row) => {
  currentDrug.value = row;
  inboundForm.quantity = 50;
  inboundForm.supplier = "";
  inboundDialogVisible.value = true;
};

const submitInbound = async () => {
  if (!inboundForm.supplier) return ElMessage.warning("请选择供应商");
  await request.post(`/api/pharmacy/drugs/${currentDrug.value.id}/inbound`, null, {
    params: { quantity: inboundForm.quantity },
  });
  ElMessage.success(
    `成功入库 ${inboundForm.quantity} ${currentDrug.value.unit} ${currentDrug.value.drugName}`,
  );
  inboundDialogVisible.value = false;
  fetchDrugs();
};

onMounted(() => {
  fetchPrescriptions();
  fetchDrugs();
});
</script>

<style scoped>
.pharmacy-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.main-card {
  border: none;
  border-radius: 4px;
  min-height: 750px;
}
.page-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}
.page-header .title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.pharmacy-tabs {
  margin-top: 10px;
}
.filter-box {
  background-color: #fafafa;
  padding: 18px 18px 0 18px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.amount-text {
  color: #f56c6c;
  font-weight: bold;
}
.text-danger {
  color: #f56c6c;
}
.font-bold {
  font-weight: bold;
}
.mb-4 {
  margin-bottom: 16px;
}
.section-title {
  margin: 0 0 15px 0;
  padding-left: 10px;
  border-left: 4px solid #409eff;
  font-size: 15px;
  color: #303133;
}
</style>
