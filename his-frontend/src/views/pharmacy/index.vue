<template>
  <div style="padding: 20px">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="处方审核与发药" name="dispense">
          <el-table
            :data="paidPrescriptions"
            v-loading="loading"
            border
            style="width: 100%"
          >
            <el-table-column prop="id" label="处方单号" width="100" />
            <el-table-column prop="patientId" label="患者ID" width="100" />
            <el-table-column prop="drugItems" label="处方明细" />
            <el-table-column
              prop="auditStatus"
              label="药师审核状态"
              width="150"
            >
              <template #default="scope">
                <el-tag
                  :type="
                    scope.row.auditStatus === 'APPROVED' ? 'success' : 'warning'
                  "
                >
                  {{
                    scope.row.auditStatus === "APPROVED"
                      ? "已审核通过"
                      : "待审核"
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="业务操作" width="180" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="scope.row.auditStatus !== 'APPROVED'"
                  type="warning"
                  size="small"
                  plain
                  @click="handleReview(scope.row)"
                  >药师审核</el-button
                >
                <el-button
                  v-if="scope.row.auditStatus === 'APPROVED'"
                  type="primary"
                  size="small"
                  @click="openDispense(scope.row)"
                  >配药出库</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="库存预警与台账" name="inventory">
          <el-row :gutter="20">
            <el-col :span="16">
              <div style="margin-bottom: 15px">
                <el-button
                  type="success"
                  icon="Plus"
                  @click="dialogVisible = true"
                  >新增药品档案</el-button
                >
              </div>
              <el-table :data="inventoryList" v-loading="loading" border>
                <el-table-column prop="code" label="药品编码" width="120" />
                <el-table-column prop="name" label="药品名称" />
                <el-table-column prop="unit" label="单位" width="80" />
                <el-table-column prop="price" label="单价(元)" width="100" />
                <el-table-column prop="stock" label="当前库存" width="100">
                  <template #default="scope">
                    <el-tag
                      :type="
                        scope.row.stock < scope.row.warningThreshold
                          ? 'danger'
                          : 'success'
                      "
                    >
                      {{ scope.row.stock }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="scope">
                    <el-button
                      size="small"
                      type="primary"
                      link
                      @click="handleInbound(scope.row)"
                      >入库</el-button
                    >
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" header="库存告急预警">
                <el-alert
                  v-for="item in warningList"
                  :key="item.id"
                  :title="`${item.name} 库存告急 (仅剩 ${item.stock} ${item.unit})`"
                  type="error"
                  show-icon
                  :closable="false"
                  style="margin-bottom: 10px"
                />
                <el-empty
                  v-if="warningList.length === 0"
                  description="暂无库存预警"
                  :image-size="60"
                />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dispenseVisible" title="处方发药核验" width="500px">
      <el-form :model="dispenseForm" label-width="100px">
        <el-form-item label="核对处方号">
          <el-input v-model="dispenseForm.prescriptionId" disabled />
        </el-form-item>
        <el-form-item label="关联库存药品">
          <el-select
            v-model="dispenseForm.drugId"
            placeholder="请选择要扣减库存的药品"
            style="width: 100%"
          >
            <el-option
              v-for="drug in inventoryList"
              :key="drug.id"
              :label="drug.name + ' (剩余:' + drug.stock + ')'"
              :value="drug.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发药数量">
          <el-input-number
            v-model="dispenseForm.quantity"
            :min="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispenseVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispense"
          >确认发药并扣减库存</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="新增药品目录" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="药品编码"
          ><el-input v-model="form.code"
        /></el-form-item>
        <el-form-item label="药品名称"
          ><el-input v-model="form.name"
        /></el-form-item>
        <el-form-item label="单位"
          ><el-input v-model="form.unit" placeholder="如：盒、瓶"
        /></el-form-item>
        <el-form-item label="零售价"
          ><el-input-number v-model="form.price" :min="0" :precision="2"
        /></el-form-item>
        <el-form-item label="初始库存"
          ><el-input-number v-model="form.stock" :min="0"
        /></el-form-item>
        <el-form-item label="预警阈值"
          ><el-input-number v-model="form.warningThreshold" :min="1"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDrug">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage, ElMessageBox } from "element-plus";

const activeTab = ref("dispense");
const loading = ref(false);
const inventoryList = ref([]);
const warningList = ref([]);
const paidPrescriptions = ref([]);

const dialogVisible = ref(false);
const dispenseVisible = ref(false);

const form = reactive({
  code: "",
  name: "",
  unit: "盒",
  price: 0,
  stock: 100,
  warningThreshold: 20,
});
const dispenseForm = reactive({
  prescriptionId: null,
  drugId: null,
  quantity: 1,
});

const fetchData = async () => {
  loading.value = true;
  try {
    // 获取处方单（过滤出已缴费且未发药完成的）
    const rxRes = await request.get("/api/doctors/prescriptions");
    paidPrescriptions.value = (rxRes || []).filter(
      (p) => p.paid === "Y" && p.dispenseStatus !== "DONE",
    );

    // 获取库存和预警数据
    const invRes = await request.get("/api/pharmacy/inventory");
    inventoryList.value = invRes.records || [];
    warningList.value =
      (await request.get("/api/pharmacy/inventory/warnings")) || [];
  } finally {
    loading.value = false;
  }
};

// 药师审核处方
const handleReview = async (row) => {
  await request.post(
    `/api/pharmacy/prescriptions/${row.id}/review?approved=true`,
  );
  ElMessage.success("处方审核通过，允许发药");
  fetchData();
};

// 准备发药
const openDispense = (row) => {
  dispenseForm.prescriptionId = row.id;
  dispenseForm.drugId = null;
  dispenseForm.quantity = 1;
  dispenseVisible.value = true;
};

// 提交发药并扣减库存
const submitDispense = async () => {
  if (!dispenseForm.drugId)
    return ElMessage.warning("请选择关联的系统库存药品");
  try {
    await request.post(
      `/api/pharmacy/prescriptions/${dispenseForm.prescriptionId}/dispense?drugId=${dispenseForm.drugId}&quantity=${dispenseForm.quantity}`,
    );
    ElMessage.success("发药完成，库存已安全扣减！");
    dispenseVisible.value = false;
    fetchData(); // 全面刷新库存与待发药列表
  } catch (error) {
    // 拦截器会处理后端的超卖库存不足异常
  }
};

const submitDrug = async () => {
  await request.post("/api/pharmacy/drugs", form);
  ElMessage.success("药品添加成功");
  dialogVisible.value = false;
  fetchData();
};

const handleInbound = (row) => {
  ElMessageBox.prompt(`请输入【${row.name}】的入库数量`, "药品入库", {
    confirmButtonText: "确认入库",
    cancelButtonText: "取消",
    inputPattern: /^[1-9]\d*$/,
    inputErrorMessage: "请输入大于0的正整数",
  })
    .then(async ({ value }) => {
      await request.post(
        `/api/pharmacy/drugs/${row.id}/inbound?quantity=${value}`,
      );
      ElMessage.success("入库成功");
      fetchData();
    })
    .catch(() => {});
};

onMounted(() => {
  fetchData();
});
</script>
