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
        :data="tableData"
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";

const queryParams = reactive({ dateRange: null });
const lineChartRef = ref(null);
const pieChartRef = ref(null);
const tableData = ref([]);

const initCharts = () => {
  const lineChart = echarts.init(lineChartRef.value);
  lineChart.setOption({
    title: { text: "近七日门诊营收趋势" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: ["06-15", "06-16", "06-17", "06-18", "06-19", "06-20", "06-21"],
    },
    yAxis: { type: "value" },
    series: [
      {
        data: [12000, 13500, 11000, 14200, 15800, 18000, 14530],
        type: "line",
        smooth: true,
        areaStyle: { opacity: 0.1 },
      },
    ],
  });

  const pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    title: { text: "营收构成占比", left: "center" },
    tooltip: { trigger: "item" },
    legend: { bottom: "0", left: "center" },
    series: [
      {
        type: "pie",
        radius: ["40%", "70%"],
        data: [
          { value: 8500, name: "处方费" },
          { value: 4200, name: "检查费" },
          { value: 1830, name: "挂号费" },
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

const handleSearch = () => {
  tableData.value = [
    {
      date: "2026-06-21",
      totalIncome: 15420.5,
      totalRefund: 890.0,
      netIncome: 14530.5,
    },
    {
      date: "2026-06-20",
      totalIncome: 18500.0,
      totalRefund: 500.0,
      netIncome: 18000.0,
    },
    {
      date: "2026-06-19",
      totalIncome: 16200.0,
      totalRefund: 400.0,
      netIncome: 15800.0,
    },
  ];
};

const exportData = () => {
  ElMessage.success("报表数据已导出为 Excel");
};

onMounted(() => {
  handleSearch();
  nextTick(() => {
    initCharts();
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
</style>
