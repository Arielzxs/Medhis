<template>
  <div class="analytics-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">科室与医生工作量统计</h3>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="统计周期">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>
          <el-form-item label="科室">
            <el-select
              v-model="queryParams.department"
              placeholder="全部科室"
              clearable
            >
              <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询统计</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-row :gutter="20">
        <el-col :span="24">
          <div class="chart-container" ref="barChartRef"></div>
        </el-col>
      </el-row>

      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%; margin-top: 20px"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column type="index" label="排名" width="80" align="center" />
        <el-table-column prop="department" label="科室" align="center" />
        <el-table-column prop="doctorName" label="医生姓名" align="center" />
        <el-table-column prop="patientCount" label="接诊人次" align="center" />
        <el-table-column
          prop="prescriptionCount"
          label="开具处方数"
          align="center"
        />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from "vue";
import * as echarts from "echarts";
import request from "../../utils/request";
import { fetchDepartmentOptions } from "../../utils/departments";

const queryParams = reactive({ dateRange: null, department: "" });
const barChartRef = ref(null);
const tableData = ref([]);
const departments = ref([]);

let myChart;
const initChart = () => {
  myChart = echarts.init(barChartRef.value);
  updateChart();
};

const updateChart = () => {
  const rows = tableData.value.slice(0, 5);
  myChart.setOption({
    title: { text: "医生接诊工作量排行 (Top 5)", left: "center" },
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: rows.map((item) => item.doctorName) },
    yAxis: { type: "value", name: "接诊人次" },
    series: [
      {
        data: rows.map((item) => item.patientCount),
        type: "bar",
        barWidth: "40%",
        itemStyle: { color: "#409EFF", borderRadius: [4, 4, 0, 0] },
      },
    ],
  });
};

const handleSearch = async () => {
  const workload = await request.get("/api/analytics/doctor-workload");
  tableData.value = Object.entries(workload || {})
    .map(([doctorName, count]) => ({
      department: queryParams.department || "--",
      doctorName,
      patientCount: count,
      prescriptionCount: 0,
    }))
    .sort((a, b) => b.patientCount - a.patientCount);
  if (myChart) updateChart();
};

onMounted(() => {
  nextTick(async () => {
    departments.value = await fetchDepartmentOptions();
    initChart();
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
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
