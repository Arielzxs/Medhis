<template>
  <div style="padding: 20px">
    <h2 style="margin-bottom: 30px">欢迎使用 HIS 医疗信息管理系统</h2>

    <el-row :gutter="20" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span
                ><el-icon><Money /></el-icon> 财务概况</span
              >
            </div>
          </template>
          <div style="font-size: 16px; color: #67c23a; line-height: 1.5">
            {{ dashboardData.financeSummary || "暂无财务数据" }}
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span
                ><el-icon><Document /></el-icon> 系统审计日志</span
              >
            </div>
          </template>
          <el-statistic :value="dashboardData.auditLogCount || 0">
            <template #suffix>条</template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span
                ><el-icon><User /></el-icon> 门诊趋势 (按日)</span
              >
            </div>
          </template>
          <div
            v-if="
              dashboardData.outpatientTrend &&
              Object.keys(dashboardData.outpatientTrend).length > 0
            "
          >
            <div
              v-for="(count, date) in dashboardData.outpatientTrend"
              :key="date"
              style="margin-bottom: 8px"
            >
              <el-tag size="small" style="margin-right: 10px">{{
                date
              }}</el-tag>
              <span>{{ count }} 人次</span>
            </div>
          </div>
          <el-empty v-else description="暂无挂号数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "../../utils/request";

const loading = ref(false);
const dashboardData = ref({});

const fetchDashboard = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/analytics/dashboard");
    dashboardData.value = res || {};
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchDashboard();
});
</script>

<style scoped>
.card-header {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
