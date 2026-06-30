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
              filterable
              style="width: 160px"
            >
              <el-option v-for="item in drugCategories" :key="item" :label="item" :value="item" />
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
        :data="pagedTableData"
        border
        stripe
        style="width: 100%; margin-top: 20px"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column prop="rank" label="排名" width="80" align="center" />
        <el-table-column prop="drugCode" label="药品编码" align="center" />
        <el-table-column prop="drugName" label="药品名称" align="center" />
        <el-table-column prop="category" label="分类" align="center" />
        <el-table-column prop="consumption" label="累计消耗数量" align="center">
          <template #default="scope">
            <el-tag effect="dark" type="danger" v-if="scope.row.rank <= 3">{{
              scope.row.consumption
            }}</el-tag>
            <span v-else>{{ scope.row.consumption }}</span>
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
import * as echarts from "echarts";
import request from "../../utils/request";

const queryParams = reactive({ dateRange: null, category: "" });
const horizontalBarChartRef = ref(null);
const tableData = ref([]);
const drugCategories = ["西药", "中成药", "中药饮片", "外用药", "注射剂", "耗材", "未分类"];
const pageState = reactive({
  page: 1,
  size: 10,
});

const pagedTableData = computed(() => {
  const start = (pageState.page - 1) * pageState.size;
  return tableData.value.slice(start, start + pageState.size);
});

let myChart;
const initChart = () => {
  myChart = echarts.init(horizontalBarChartRef.value);
  updateChart();
};

const updateChart = () => {
  const rows = tableData.value.slice(0, 5).reverse();
  myChart.setOption({
    title: { text: "药品消耗数量 Top 5" },
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
    xAxis: { type: "value", boundaryGap: [0, 0.01] },
    yAxis: {
      type: "category",
      data: rows.map((item) => item.drugName),
    },
    series: [
      {
        name: "消耗数量",
        type: "bar",
        data: rows.map((item) => item.consumption),
        itemStyle: { color: "#67C23A", borderRadius: [0, 4, 4, 0] },
      },
    ],
  });
};

const formatDate = (value) => {
  if (!value) return undefined;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toISOString().slice(0, 10);
};

const handleSearch = async () => {
  const [startDate, endDate] = queryParams.dateRange || [];
  const rows = await request.get("/api/analytics/drug-consumption", {
    params: {
      category: queryParams.category || undefined,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
    },
  });
  tableData.value = (rows || [])
    .map((item) => ({
      drugCode: item.drugCode || "--",
      drugName: item.drugName || "--",
      category: item.category || "未分类",
      consumption: Number(item.consumption || 0),
    }))
    .sort((a, b) => b.consumption - a.consumption)
    .map((item, index) => ({
      ...item,
      rank: index + 1,
    }));
  pageState.page = 1;
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

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
