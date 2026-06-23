<template>
  <div class="analytics-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">药品消耗数据排行</h3>
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
          <el-form-item label="药品分类">
            <el-select
              v-model="queryParams.category"
              placeholder="全部分类"
              clearable
            >
              <el-option label="西药" value="西药" />
              <el-option label="中成药" value="中成药" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询排行</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-row :gutter="20">
        <el-col :span="24">
          <div class="chart-container" ref="horizontalBarChartRef"></div>
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
        <el-table-column prop="drugCode" label="药品编码" align="center" />
        <el-table-column prop="drugName" label="药品名称" align="center" />
        <el-table-column prop="category" label="分类" align="center" />
        <el-table-column prop="consumption" label="累计消耗数量" align="center">
          <template #default="scope">
            <el-tag effect="dark" type="danger" v-if="scope.$index < 3">{{
              scope.row.consumption
            }}</el-tag>
            <span v-else>{{ scope.row.consumption }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from "vue";
import * as echarts from "echarts";
import request from "../../utils/request";

const queryParams = reactive({ dateRange: null, category: "" });
const horizontalBarChartRef = ref(null);
const tableData = ref([]);

let myChart;
const initChart = () => {
  myChart = echarts.init(horizontalBarChartRef.value);
  updateChart();
};

const updateChart = () => {
  const rows = tableData.value.slice(0, 5).reverse();
  myChart.setOption({
    title: { text: "药品库存数量 Top 5" },
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
    xAxis: { type: "value", boundaryGap: [0, 0.01] },
    yAxis: {
      type: "category",
      data: rows.map((item) => item.drugName),
    },
    series: [
      {
        name: "库存数量",
        type: "bar",
        data: rows.map((item) => item.consumption),
        itemStyle: { color: "#67C23A", borderRadius: [0, 4, 4, 0] },
      },
    ],
  });
};

const handleSearch = async () => {
  const res = await request.get("/api/pharmacy/inventory", {
    params: { page: 1, size: 100 },
  });
  tableData.value = (res.records || [])
    .map((item) => ({
      drugCode: item.code,
      drugName: item.name,
      category: "--",
      consumption: item.stock || 0,
    }))
    .sort((a, b) => b.consumption - a.consumption);
  if (myChart) updateChart();
};

onMounted(() => {
  nextTick(async () => {
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
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
