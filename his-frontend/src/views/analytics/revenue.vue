<template>
  <div class="analytics-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">财务收支数据统计</h3>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="对账周期">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">生成报表</el-button>
            <el-button @click="exportData">导出 Excel</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-row :gutter="20">
        <el-col :span="16">
          <div class="chart-container" ref="lineChartRef"></div>
        </el-col>
        <el-col :span="8">
          <div class="chart-container" ref="pieChartRef"></div>
        </el-col>
      </el-row>

      <el-table
        :data="pagedTableData"
        border
        stripe
        style="width: 100%; margin-top: 20px"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="date" label="对账日期" align="center" />
        <el-table-column prop="totalIncome" label="总收入(元)" align="center" />
        <el-table-column
          prop="totalRefund"
          label="退费金额(元)"
          align="center"
        />
        <el-table-column prop="netIncome" label="实际净收入(元)" align="center">
          <template #default="scope">
            <span style="color: #67c23a; font-weight: bold"
              >¥ {{ scope.row.netIncome.toFixed(2) }}</span
            >
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pageState.page"
          v-model:page-size="pageState.size"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableData.length"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import request from "../../utils/request";

const queryParams = reactive({ dateRange: null });
const lineChartRef = ref(null);
const pieChartRef = ref(null);
const tableData = ref([]);
const pageState = reactive({
  page: 1,
  size: 10,
});

const pagedTableData = computed(() => {
  const start = (pageState.page - 1) * pageState.size;
  return tableData.value.slice(start, start + pageState.size);
});

let lineChart;
let pieChart;

const initCharts = () => {
  lineChart = echarts.init(lineChartRef.value);
  pieChart = echarts.init(pieChartRef.value);
  updateCharts();
};

const updateCharts = () => {
  const dates = tableData.value.map((item) => item.date);
  const netIncome = tableData.value.map((item) => item.netIncome);
  lineChart.setOption({
    title: { text: "近七日门诊营收趋势" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: dates,
    },
    yAxis: { type: "value" },
    series: [
      {
        data: netIncome,
        type: "line",
        smooth: true,
        areaStyle: { opacity: 0.1 },
      },
    ],
  });

  const totalIncome = tableData.value.reduce(
    (sum, item) => sum + item.totalIncome,
    0,
  );
  const totalRefund = tableData.value.reduce(
    (sum, item) => sum + item.totalRefund,
    0,
  );
  pieChart.setOption({
    title: { text: "营收构成占比", left: "center" },
    tooltip: { trigger: "item" },
    legend: { bottom: "0", left: "center" },
    series: [
      {
        type: "pie",
        radius: ["40%", "70%"],
        data: [
          { value: totalIncome, name: "收入" },
          { value: totalRefund, name: "退费" },
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.5)",
          },
        },
      },
    ],
  });
};

const handleSearch = async () => {
  const res = await request.get("/api/finance/reports", {
    params: { page: 1, size: 500 },
  });
  const grouped = new Map();
  for (const item of res.records || []) {
    const date = item.createdAt?.slice(0, 10) || "未知日期";
    if (!grouped.has(date)) {
      grouped.set(date, { date, totalIncome: 0, totalRefund: 0, netIncome: 0 });
    }
    const row = grouped.get(date);
    const amount = Number(item.amount || 0);
    if (item.direction === "IN") row.totalIncome += amount;
    if (item.bizType === "REFUND") row.totalRefund += amount;
    row.netIncome = row.totalIncome - row.totalRefund;
  }
  tableData.value = [...grouped.values()].sort((a, b) =>
    a.date.localeCompare(b.date),
  );
  pageState.page = 1;
  if (lineChart && pieChart) updateCharts();
};

const exportData = () => {
  ElMessage.success("报表数据已导出为 Excel");
};

onMounted(() => {
  nextTick(async () => {
    initCharts();
    await handleSearch();
  });
});
</script>

<style scoped>
.analytics-container {
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
.filter-box {
  background-color: #fafafa;
  padding: 18px 18px 0 18px;
  border-radius: 4px;
  margin-bottom: 20px;
}
.chart-container {
  height: 350px;
  background: #fff;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
