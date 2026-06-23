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
                <el-button type="info" link size="small">编辑</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

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

const fetchPrescriptions = () => {
  loadingDispense.value = true;
  setTimeout(() => {
    prescriptionList.value = [
      {
        id: 1,
        prescriptionNo: "RX260621001",
        patientName: "李建国",
        doctorName: "王鹏",
        department: "心血管内科",
        amount: 156.5,
        status: "待审核",
        createTime: "2026-06-21 09:40:12",
        items: [
          {
            drugName: "阿司匹林肠溶片",
            specification: "100mg*30片",
            quantity: 2,
            usage: "每日一次，每次一片",
          },
        ],
      },
      {
        id: 2,
        prescriptionNo: "RX260621002",
        patientName: "王芳",
        doctorName: "孙芳",
        department: "儿科",
        amount: 45.0,
        status: "待发药",
        createTime: "2026-06-21 10:20:00",
        items: [
          {
            drugName: "头孢克肟颗粒",
            specification: "50mg*10袋",
            quantity: 1,
            usage: "每日两次，每次一袋",
          },
        ],
      },
      {
        id: 3,
        prescriptionNo: "RX260620015",
        patientName: "张伟",
        doctorName: "李静",
        department: "消化内科",
        amount: 120.0,
        status: "已发药",
        createTime: "2026-06-20 14:30:00",
        items: [],
      },
      {
        id: 4,
        prescriptionNo: "RX260620016",
        patientName: "赵小明",
        doctorName: "赵强",
        department: "普外科",
        amount: 80.0,
        status: "已退回",
        createTime: "2026-06-20 15:45:00",
        items: [],
      },
    ];
    loadingDispense.value = false;
  }, 400);
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

const submitReview = (isPass) => {
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
    .then(() => {
      ElMessage.success("发药成功，库存已自动扣减");
      row.status = "已发药";
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
const currentDrug = ref(null);
const inboundForm = reactive({ quantity: 50, supplier: "" });

const fetchDrugs = () => {
  loadingInventory.value = true;
  setTimeout(() => {
    drugList.value = [
      {
        id: 1,
        drugCode: "D10021",
        drugName: "阿莫西林胶囊",
        category: "西药",
        specification: "0.25g*50粒",
        unit: "盒",
        price: 15.5,
        stock: 320,
        warningThreshold: 100,
      },
      {
        id: 2,
        drugCode: "D10022",
        drugName: "连花清瘟胶囊",
        category: "中成药",
        specification: "0.35g*24粒",
        unit: "盒",
        price: 29.8,
        stock: 45,
        warningThreshold: 50,
      },
      {
        id: 3,
        drugCode: "D10023",
        drugName: "阿司匹林肠溶片",
        category: "西药",
        specification: "100mg*30片",
        unit: "瓶",
        price: 12.0,
        stock: 20,
        warningThreshold: 50,
      },
      {
        id: 4,
        drugCode: "D10024",
        drugName: "布洛芬缓释胶囊",
        category: "西药",
        specification: "0.3g*22粒",
        unit: "盒",
        price: 18.5,
        stock: 150,
        warningThreshold: 50,
      },
    ];
    loadingInventory.value = false;
  }, 400);
};

const filteredDrugList = computed(() => {
  if (inventoryQuery.showWarningOnly) {
    return drugList.value.filter((d) => d.stock <= d.warningThreshold);
  }
  return drugList.value;
});

const handleAddDrug = () => {
  ElMessage.success("跳转至新增药品表单");
};

const openInboundDialog = (row) => {
  currentDrug.value = row;
  inboundForm.quantity = 50;
  inboundForm.supplier = "";
  inboundDialogVisible.value = true;
};

const submitInbound = () => {
  if (!inboundForm.supplier) return ElMessage.warning("请选择供应商");
  currentDrug.value.stock += inboundForm.quantity;
  ElMessage.success(
    `成功入库 ${inboundForm.quantity} ${currentDrug.value.unit} ${currentDrug.value.drugName}`,
  );
  inboundDialogVisible.value = false;
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
