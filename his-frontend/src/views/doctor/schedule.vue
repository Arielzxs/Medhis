<!-- src/views/doctor/schedule.vue -->
<template>
  <div class="schedule-container">
    <el-card shadow="never" class="schedule-card">
      <!-- 顶部标题与新增按钮 -->
      <div class="header-action">
        <span class="title">人员排班管理</span>
        <el-button type="primary" @click="handleAdd">新增排班</el-button>
      </div>

      <!-- 筛选表单 -->
      <div class="filter-box">
        <el-form :inline="true" :model="queryParams" class="demo-form-inline">
          <el-form-item label="出诊科室">
            <el-select
              v-model="queryParams.department"
              placeholder="请选择科室"
              clearable
              style="width: 160px"
            >
              <el-option label="心血管内科" value="心血管内科" />
              <el-option label="儿科" value="儿科" />
              <el-option label="消化内科" value="消化内科" />
              <el-option label="普外科" value="普外科" />
            </el-select>
          </el-form-item>
          <el-form-item label="医生姓名">
            <el-input
              v-model="queryParams.doctorName"
              placeholder="请输入医生姓名"
              clearable
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="排班日期">
            <el-date-picker
              v-model="queryParams.date"
              type="date"
              placeholder="选择日期"
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column
          prop="scheduleDate"
          label="排班日期"
          align="center"
          width="130"
        />
        <el-table-column prop="shift" label="班次" align="center" width="90">
          <template #default="scope">
            <span
              :style="{
                color: scope.row.shift === '上午' ? '#67c23a' : '#e6a23c',
              }"
            >
              {{ scope.row.shift }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="出诊科室" align="center" />
        <el-table-column prop="doctorName" label="医生姓名" align="center" />
        <el-table-column prop="level" label="号别" align="center" width="120" />
        <el-table-column
          prop="limit"
          label="挂号限额"
          align="center"
          width="100"
        />
        <el-table-column prop="status" label="状态" align="center" width="120">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="正常"
              inactive-text="停诊"
              inline-prompt
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";

const loading = ref(false);
const queryParams = reactive({
  department: "",
  doctorName: "",
  date: "",
});

const tableData = ref([]);

const fetchSchedules = () => {
  loading.value = true;
  setTimeout(() => {
    tableData.value = [
      {
        id: 1,
        scheduleDate: "2026-06-11",
        shift: "上午",
        department: "心血管内科",
        doctorName: "王鹏",
        level: "专家号",
        limit: 30,
        status: 1,
      },
      {
        id: 2,
        scheduleDate: "2026-06-11",
        shift: "下午",
        department: "儿科",
        doctorName: "孙芳",
        level: "普通号",
        limit: 50,
        status: 1,
      },
      {
        id: 3,
        scheduleDate: "2026-06-12",
        shift: "上午",
        department: "消化内科",
        doctorName: "李静",
        level: "专家号",
        limit: 25,
        status: 0,
      },
      {
        id: 4,
        scheduleDate: "2026-06-12",
        shift: "下午",
        department: "普外科",
        doctorName: "赵强",
        level: "普通号",
        limit: 40,
        status: 1,
      },
    ];
    loading.value = false;
  }, 400);
};

const handleSearch = () => {
  fetchSchedules();
};

const resetQuery = () => {
  queryParams.department = "";
  queryParams.doctorName = "";
  queryParams.date = "";
  fetchSchedules();
};

const handleAdd = () => {
  ElMessage.success("点击了新增排班");
};

onMounted(() => {
  fetchSchedules();
});
</script>

<style scoped>
.schedule-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.schedule-card {
  border: none;
  border-radius: 4px;
  min-height: 750px;
}

.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}
.header-action .title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.filter-box {
  margin-bottom: 20px;
}
</style>
