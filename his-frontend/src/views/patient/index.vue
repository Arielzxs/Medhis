<template>
  <div>
    <el-card>
      <div style="margin-bottom: 20px; display: flex; gap: 10px">
        <el-input
          v-model="queryParams.keyword"
          placeholder="患者姓名/病历号"
          style="width: 250px"
          clearable
        />
        <el-button type="primary" icon="Search" @click="fetchData"
          >搜索</el-button
        >
        <el-button type="success" icon="Plus" @click="dialogVisible = true"
          >新建患者档案</el-button
        >
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        style="width: 100%"
      >
        <el-table-column prop="patientNo" label="患者编号" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="currentStatus" label="状态" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建档案" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名"
          ><el-input v-model="form.name"
        /></el-form-item>
        <el-form-item label="编号"
          ><el-input v-model="form.patientNo" placeholder="请输入唯一编号"
        /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage } from "element-plus";

const loading = ref(false);
const tableData = ref([]);
const queryParams = reactive({ keyword: "", page: 1, size: 10 });

const dialogVisible = ref(false);
const form = reactive({ name: "", patientNo: "", gender: "男" });

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/patients", { params: queryParams });
    tableData.value = res.records || [];
  } finally {
    loading.value = false;
  }
};

const submitSave = async () => {
  await request.post("/api/patients", form);
  ElMessage.success("建档成功");
  dialogVisible.value = false;
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>
