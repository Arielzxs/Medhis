<template>
  <el-card>
    <template #header>财务流水明细</template>
    <el-table :data="reports" v-loading="loading" border style="width: 100%">
      <el-table-column prop="bizType" label="业务类型" width="180" />
      <el-table-column prop="amount" label="交易金额(元)" width="120" />
      <el-table-column prop="direction" label="收支方向" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.direction === 'IN' ? 'success' : 'danger'">
            {{ scope.row.direction === "IN" ? "收入" : "支出" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="发生时间" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "../../utils/request";

const reports = ref([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/finance/reports");
    reports.value = res.records || [];
  } finally {
    loading.value = false;
  }
});
</script>
