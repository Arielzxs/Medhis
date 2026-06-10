<template>
  <div style="padding: 20px">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="门诊收费 (待处理)" name="charge">
          <div style="margin-bottom: 15px; color: #e6a23c; font-size: 13px">
            <el-icon><InfoFilled /></el-icon> 仅显示医生已开具且未缴费的处方单
          </div>
          <el-table
            :data="unpaidList"
            v-loading="loading"
            border
            style="width: 100%"
          >
            <el-table-column prop="id" label="处方单号" width="100" />
            <el-table-column prop="patientId" label="患者ID" width="100" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="drugItems" label="药品/项目明细" />
            <el-table-column prop="totalAmount" label="总金额(元)" width="120">
              <template #default="scope">
                <span style="color: #f56c6c; font-weight: bold"
                  >¥ {{ scope.row.totalAmount }}</span
                >
              </template>
            </el-table-column>
            <el-table-column label="业务操作" width="150" fixed="right">
              <template #default="scope">
                <el-button
                  type="success"
                  size="small"
                  @click="handleCharge(scope.row)"
                  >划价并收费</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="财务流水明细" name="report">
          <el-table
            :data="reports"
            v-loading="loading"
            border
            style="width: 100%"
          >
            <el-table-column prop="id" label="流水号" width="180" />
            <el-table-column prop="bizType" label="业务类型" width="180" />
            <el-table-column prop="amount" label="交易金额(元)" width="120" />
            <el-table-column prop="direction" label="收支方向" width="100">
              <template #default="scope">
                <el-tag
                  :type="scope.row.direction === 'IN' ? 'success' : 'danger'"
                >
                  {{ scope.row.direction === "IN" ? "收入" : "支出" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="createdAt" label="发生时间" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage, ElMessageBox } from "element-plus";

const activeTab = ref("charge");
const reports = ref([]);
const unpaidList = ref([]);
const loading = ref(false);

const fetchData = async () => {
  loading.value = true;
  try {
    // 拉取全部处方，并在前端过滤出未缴费的
    const rxRes = await request.get("/api/doctors/prescriptions");
    unpaidList.value = (rxRes || []).filter((p) => p.paid === "N");

    // 拉取财务流水
    const repRes = await request.get("/api/finance/reports");
    reports.value = repRes.records || [];
  } finally {
    loading.value = false;
  }
};

const handleCharge = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认收取患者处方费用 ¥${row.totalAmount} 元？`,
      "收费确认",
      {
        confirmButtonText: "微信/支付宝收款",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    loading.value = true;
    // 1. 划价生成账单
    const bill = await request.post(
      `/api/finance/prescriptions/${row.id}/price`,
    );
    // 2. 模拟扫码收款成功并核销账单
    await request.post(
      `/api/finance/bills/${bill.id}/charge?channel=WECHAT&settlementType=SELF_PAY`,
    );
    // 3. 将处方标记为已缴费，推送至药房
    await request.post(`/api/finance/prescriptions/${row.id}/mark-paid`);

    ElMessage.success("收费结算成功，患者可前往药房取药");
    fetchData(); // 刷新列表
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("收费流程异常");
    }
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>
