<template>
  <div class="finance-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">财务管理工作站</h3>
      </div>

      <el-tabs v-model="activeTab" class="finance-tabs">
        <el-tab-pane label="门诊收费结算" name="charge">
          <div class="filter-box">
            <el-form :inline="true" :model="chargeQuery">
              <el-form-item label="患者就诊号">
                <el-input
                  v-model="chargeQuery.patientNo"
                  placeholder="请输入就诊号/挂号单号"
                  clearable
                  style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="患者姓名">
                <el-input
                  v-model="chargeQuery.patientName"
                  placeholder="请输入患者姓名"
                  clearable
                  style="width: 150px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchPendingBills"
                  >查询待缴单据</el-button
                >
                <el-button @click="resetChargeQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="pendingBills"
            border
            stripe
            style="width: 100%"
            v-loading="loadingCharge"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <el-table-column
              prop="billNo"
              label="单据编号"
              align="center"
              width="150"
            />
            <el-table-column
              prop="patientName"
              label="患者姓名"
              align="center"
              width="100"
            />
            <el-table-column
              prop="billType"
              label="费用类型"
              align="center"
              width="120"
            >
              <template #default="scope">
                <el-tag
                  :type="
                    scope.row.billType === '挂号费'
                      ? 'info'
                      : scope.row.billType === '处方费'
                        ? 'success'
                        : 'warning'
                  "
                >
                  {{ scope.row.billType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="department"
              label="开单科室"
              align="center"
              width="120"
            />
            <el-table-column
              prop="doctorName"
              label="开单医生"
              align="center"
              width="100"
            />
            <el-table-column
              prop="amount"
              label="应收金额(元)"
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
              prop="createTime"
              label="开单时间"
              align="center"
            />
            <el-table-column
              label="操作"
              align="center"
              width="120"
              fixed="right"
            >
              <template #default="scope">
                <el-button
                  type="success"
                  size="small"
                  @click="openChargeDialog(scope.row)"
                  >结算收款</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              v-model:current-page="chargePage.page"
              v-model:page-size="chargePage.size"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="chargePage.total"
              @current-change="fetchPendingBills"
              @size-change="handleChargePageSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="门诊退费管理" name="refund">
          <div class="filter-box">
            <el-form :inline="true" :model="refundQuery">
              <el-form-item label="原始结算单号">
                <el-input
                  v-model="refundQuery.billNo"
                  placeholder="请输入结算单号"
                  clearable
                  style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="患者姓名">
                <el-input
                  v-model="refundQuery.patientName"
                  placeholder="请输入患者姓名"
                  clearable
                  style="width: 150px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchPaidBills"
                  >查询已缴单据</el-button
                >
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="paidBills"
            border
            stripe
            style="width: 100%"
            v-loading="loadingRefund"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <el-table-column
              prop="billNo"
              label="结算单号"
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
              prop="billType"
              label="费用类型"
              align="center"
              width="100"
            />
            <el-table-column
              prop="amount"
              label="实收金额(元)"
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
              prop="payChannel"
              label="支付渠道"
              align="center"
              width="100"
            />
            <el-table-column prop="payTime" label="缴费时间" align="center" />
            <el-table-column
              label="操作"
              align="center"
              width="120"
              fixed="right"
            >
              <template #default="scope">
                <el-button
                  type="danger"
                  size="small"
                  plain
                  @click="openRefundDialog(scope.row)"
                  >发起退费</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              v-model:current-page="refundPage.page"
              v-model:page-size="refundPage.size"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="refundPage.total"
              @current-change="fetchPaidBills"
              @size-change="handleRefundPageSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="日结对账" name="daily">
          <div class="filter-box">
            <el-form :inline="true">
              <el-form-item label="对账日期">
                <el-date-picker
                  v-model="dailyDate"
                  type="date"
                  placeholder="选择对账日期"
                  style="width: 200px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchDailyStats"
                  >生成日结报表</el-button
                >
                <el-button type="success" plain @click="executeDailyReconcile"
                  >执行日结核对</el-button
                >
              </el-form-item>
            </el-form>
          </div>

          <el-row :gutter="20" class="stat-cards">
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card income">
                <div class="stat-title">当日总收入</div>
                <div class="stat-value">
                  ¥ {{ dailyStats.totalIncome.toFixed(2) }}
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card refund">
                <div class="stat-title">当日退费总额</div>
                <div class="stat-value">
                  ¥ {{ dailyStats.totalRefund.toFixed(2) }}
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="stat-card net">
                <div class="stat-title">实际净营收</div>
                <div class="stat-value">
                  ¥ {{ dailyStats.netIncome.toFixed(2) }}
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="chargeDialogVisible" title="门诊收费结算" width="500px">
      <el-form :model="chargeForm" label-width="100px">
        <el-form-item label="患者姓名：">
          <span>{{ currentBill?.patientName }}</span>
        </el-form-item>
        <el-form-item label="费用类型：">
          <span>{{ currentBill?.billType }}</span>
        </el-form-item>
        <el-form-item label="应付金额：">
          <span class="amount-large"
            >¥ {{ currentBill?.amount.toFixed(2) }}</span
          >
        </el-form-item>
        <el-divider border-style="dashed" />
        <el-form-item label="结算类型：">
          <el-radio-group v-model="chargeForm.settlementType">
            <el-radio label="自费">自费结算</el-radio>
            <el-radio label="医保">医保统筹</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付渠道：">
          <el-select
            v-model="chargeForm.payChannel"
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option label="微信支付" value="微信" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="现金" value="现金" />
            <el-option label="银行卡" value="银行卡" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="chargeDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitCharge">确认收款</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialogVisible" title="门诊单据退费" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="结算单号：">
          <span>{{ currentRefundBill?.billNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额：">
          <span class="amount-large text-danger"
            >¥ {{ currentRefundBill?.amount.toFixed(2) }}</span
          >
        </el-form-item>
        <el-form-item label="退款渠道：">
          <span>原路退回 ({{ currentRefundBill?.payChannel }})</span>
        </el-form-item>
        <el-form-item label="退费事由：">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入规范的退费事由..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="refundDialogVisible = false">取 消</el-button>
          <el-button type="danger" @click="submitRefund">确认退费</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../utils/request";

// Tab 控制
const activeTab = ref("charge");

// =========== 门诊收费结算逻辑 ===========
const loadingCharge = ref(false);
const chargeQuery = reactive({ patientNo: "", patientName: "" });
const pendingBills = ref([]);
const chargePage = reactive({ page: 1, size: 10, total: 0 });
const chargeDialogVisible = ref(false);
const currentBill = ref(null);
const chargeForm = reactive({ settlementType: "自费", payChannel: "微信" });

const billTypeText = (type) => {
  const map = {
    REGISTRATION: "挂号费",
    PRESCRIPTION: "处方费",
    CHECK: "检查费",
  };
  return map[type] || type || "费用";
};

const mapBill = (bill) => ({
  id: bill.id,
  billNo: `BL${bill.id}`,
  patientName: bill.patientName
    ? `${bill.patientName}${bill.patientNo ? `（${bill.patientNo}）` : ""}`
    : "--",
  billType: billTypeText(bill.billingType),
  department: "--",
  doctorName: "--",
  amount: Number(bill.amount || 0),
  createTime: bill.createdAt || "",
  payChannel: bill.payChannel || "--",
  payTime: bill.updatedAt || bill.createdAt || "",
});

const fetchPendingBills = async () => {
  loadingCharge.value = true;
  try {
    const res = await request.get("/api/finance/bills", {
      params: {
        status: "PRICED",
        keyword: chargeQuery.patientNo || undefined,
        patientKeyword: chargeQuery.patientName || undefined,
        page: chargePage.page,
        size: chargePage.size,
      },
    });
    pendingBills.value = (res.records || []).map(mapBill);
    chargePage.total = res.total || 0;
  } finally {
    loadingCharge.value = false;
  }
};

const resetChargeQuery = () => {
  chargeQuery.patientNo = "";
  chargeQuery.patientName = "";
  chargePage.page = 1;
  fetchPendingBills();
};

const handleChargePageSizeChange = () => {
  chargePage.page = 1;
  fetchPendingBills();
};

const openChargeDialog = (row) => {
  currentBill.value = row;
  chargeForm.settlementType = "自费";
  chargeForm.payChannel = "微信";
  chargeDialogVisible.value = true;
};

const submitCharge = async () => {
  await request.post(`/api/finance/bills/${currentBill.value.id}/charge`, null, {
    params: {
      channel: chargeForm.payChannel,
      settlementType: chargeForm.settlementType,
    },
  });
  ElMessage.success(
    `单据 ${currentBill.value.billNo} 收款成功！金额 ¥${currentBill.value.amount.toFixed(2)}`,
  );
  chargeDialogVisible.value = false;
  fetchPendingBills(); // 刷新列表
};

// =========== 门诊退费管理逻辑 ===========
const loadingRefund = ref(false);
const refundQuery = reactive({ billNo: "", patientName: "" });
const paidBills = ref([]);
const refundPage = reactive({ page: 1, size: 10, total: 0 });
const refundDialogVisible = ref(false);
const currentRefundBill = ref(null);
const refundForm = reactive({ reason: "" });

const fetchPaidBills = async () => {
  loadingRefund.value = true;
  try {
    const res = await request.get("/api/finance/bills", {
      params: {
        status: "PAID",
        keyword: refundQuery.billNo || undefined,
        patientKeyword: refundQuery.patientName || undefined,
        page: refundPage.page,
        size: refundPage.size,
      },
    });
    paidBills.value = (res.records || []).map(mapBill);
    refundPage.total = res.total || 0;
  } finally {
    loadingRefund.value = false;
  }
};

const handleRefundPageSizeChange = () => {
  refundPage.page = 1;
  fetchPaidBills();
};

const openRefundDialog = (row) => {
  currentRefundBill.value = row;
  refundForm.reason = "";
  refundDialogVisible.value = true;
};

const submitRefund = async () => {
  if (!refundForm.reason) {
    return ElMessage.warning("请输入退费事由");
  }
  await request.post(`/api/finance/bills/${currentRefundBill.value.id}/refund`, null, {
    params: { reason: refundForm.reason },
  });
  ElMessage.success(
    `单据 ${currentRefundBill.value.billNo} 已成功退费并生成流水！`,
  );
  refundDialogVisible.value = false;
  fetchPaidBills(); // 刷新列表
};

// =========== 日结对账逻辑 ===========
const dailyDate = ref(new Date());
const dailyStats = reactive({ totalIncome: 0, totalRefund: 0, netIncome: 0 });

const fetchDailyStats = async (showMessage = true) => {
  const date = dailyDate.value instanceof Date
    ? dailyDate.value.toISOString().slice(0, 10)
    : "";
  const res = await request.get("/api/finance/reports", {
    params: { date, page: 1, size: 200 },
  });
  const records = res.records || [];
  dailyStats.totalIncome = records
    .filter((item) => item.direction === "IN")
    .reduce((sum, item) => sum + Number(item.amount || 0), 0);
  dailyStats.totalRefund = records
    .filter((item) => item.bizType === "REFUND")
    .reduce((sum, item) => sum + Number(item.amount || 0), 0);
  dailyStats.netIncome = dailyStats.totalIncome - dailyStats.totalRefund;
  if (showMessage) {
    ElMessage.success("报表生成成功");
  }
};

const executeDailyReconcile = () => {
  ElMessageBox.confirm(
    "执行日结后当日账目将锁定不可更改，是否确认执行对账？",
    "日结对账确认",
    {
      confirmButtonText: "确认执行",
      cancelButtonText: "取消",
      type: "warning",
    },
  )
    .then(() => {
      request.post("/api/finance/reconcile/daily").then((message) => {
        ElMessage.success(message || "日结对账执行成功，账目已锁定。");
      });
    })
    .catch(() => {});
};

onMounted(() => {
  fetchPendingBills();
  fetchPaidBills();
  fetchDailyStats(false);
});
</script>

<style scoped>
.finance-container {
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

.finance-tabs {
  margin-top: 10px;
}
.filter-box {
  background-color: #fafafa;
  padding: 18px 18px 0 18px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.amount-text {
  color: #f56c6c;
  font-weight: bold;
}
.amount-large {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}
.text-danger {
  color: #f56c6c;
}

.stat-cards {
  margin-top: 30px;
}
.stat-card {
  text-align: center;
  padding: 20px 0;
  border-radius: 8px;
}
.stat-card.income {
  border-top: 4px solid #67c23a;
}
.stat-card.refund {
  border-top: 4px solid #f56c6c;
}
.stat-card.net {
  border-top: 4px solid #409eff;
}
.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
</style>
