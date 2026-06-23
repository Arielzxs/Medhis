<template>
  <div class="tracking-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">就诊状态追踪查询</h3>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="挂号单号">
            <el-input
              v-model="queryParams.regNo"
              placeholder="请输入单号"
              clearable
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item label="患者姓名">
            <el-input
              v-model="queryParams.patientName"
              placeholder="请输入姓名"
              clearable
              style="width: 140px"
            />
          </el-form-item>
          <el-form-item label="挂号日期">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="queryParams.status"
              placeholder="全部状态"
              clearable
              style="width: 120px"
            >
              <el-option label="待缴费" value="待缴费" />
              <el-option label="待诊" value="待诊" />
              <el-option label="待诊中" value="待诊中" />
              <el-option label="就诊中" value="就诊中" />
              <el-option label="已完成" value="已完成" />
              <el-option label="已退号" value="已退号" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleSearch"
              >查询</el-button
            >
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column
          prop="regNo"
          label="挂号单号"
          align="center"
          width="140"
        />
        <el-table-column
          prop="patientName"
          label="患者姓名"
          align="center"
          width="100"
        />
        <el-table-column
          prop="department"
          label="挂号科室"
          align="center"
          width="120"
        />
        <el-table-column
          prop="doctorName"
          label="看诊医生"
          align="center"
          width="100"
        />
        <el-table-column
          prop="scheduleDate"
          label="预计就诊时间"
          align="center"
          width="160"
        />
        <el-table-column
          prop="fee"
          label="挂号费(元)"
          align="center"
          width="100"
        >
          <template #default="scope">
            <span class="fee-text">¥{{ scope.row.fee.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="当前状态"
          align="center"
          width="120"
        >
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="light">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              link
              size="small"
              @click="printTicket(scope.row)"
              >打印小票</el-button
            >
            <el-popconfirm
              title="确定要为该患者执行退号操作吗？"
              confirm-button-text="确认退号"
              cancel-button-text="取消"
              @confirm="cancelRegistration(scope.row)"
              v-if="['待缴费', '待诊', '待诊中'].includes(scope.row.status)"
            >
              <template #reference>
                <el-button type="danger" link size="small">取消挂号</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="queryParams.page"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const queryParams = reactive({
  regNo: "",
  patientName: "",
  dateRange: null,
  status: "",
  page: 1,
});

const tableData = ref([]);
const total = ref(0);

const fetchRecords = async () => {
  loading.value = true;
  try {
    const id = queryParams.regNo?.replace(/^REG/i, "");
    const res = await request.get("/api/patients/registrations", {
      params: {
        id: id || undefined,
        status: queryParams.status || undefined,
        page: queryParams.page,
        size: 10,
      },
    });
    tableData.value = (res.records || []).filter((item) =>
      queryParams.patientName
        ? item.patientName?.includes(queryParams.patientName)
        : true,
    );
    total.value = res.total || tableData.value.length;
  } finally {
    loading.value = false;
  }
};

const getStatusType = (status) => {
  const map = {
    待缴费: "warning",
    待诊: "primary",
    待诊中: "primary",
    就诊中: "danger",
    已完成: "success",
    已退号: "info",
  };
  return map[status] || "info";
};

const handleSearch = () => {
  fetchRecords();
};

const resetQuery = () => {
  queryParams.regNo = "";
  queryParams.patientName = "";
  queryParams.dateRange = null;
  queryParams.status = "";
  queryParams.page = 1;
  fetchRecords();
};

const printTicket = async (row) => {
  const content = await request.get(`/api/patients/registrations/${row.id}/print`);
  ElMessage.success(content || `挂号单 ${row.regNo} 打印指令已发送`);
};

const cancelRegistration = async (row) => {
  await request.post(`/api/patients/registrations/${row.id}/cancel`);
  ElMessage.success(`挂号单 ${row.regNo} 已成功退号`);
  fetchRecords();
};

onMounted(() => {
  fetchRecords();
});
</script>

<style scoped>
.tracking-container {
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
.fee-text {
  color: #f56c6c;
  font-weight: bold;
}

.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
