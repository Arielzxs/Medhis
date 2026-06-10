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
        <el-table-column prop="currentStatus" label="状态" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.currentStatus === '待就诊' ? 'warning' : 'info'"
            >
              {{ scope.row.currentStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务操作" width="150" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              plain
              @click="openRegisterDialog(scope.row)"
              >挂号</el-button
            >
          </template>
        </el-table-column>
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

    <el-dialog v-model="registerVisible" title="门诊挂号" width="500px">
      <el-form :model="regForm" label-width="80px">
        <el-form-item label="挂号科室">
          <el-select v-model="regForm.department" style="width: 100%">
            <el-option label="内科" value="内科" />
            <el-option label="外科" value="外科" />
            <el-option label="儿科" value="儿科" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生ID">
          <el-input-number
            v-model="regForm.doctorId"
            :min="1"
            style="width: 100%"
            placeholder="输入排班医生ID"
          />
        </el-form-item>
        <el-form-item label="就诊日期">
          <el-date-picker
            v-model="regForm.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="挂号费(元)">
          <el-input-number
            v-model="regForm.fee"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegister">确认挂号</el-button>
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

const registerVisible = ref(false);
const regForm = reactive({
  patientId: null,
  doctorId: 1,
  department: "内科",
  scheduleDate: "",
  fee: 50.0,
});

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

const openRegisterDialog = (row) => {
  regForm.patientId = row.id;
  regForm.scheduleDate = new Date().toISOString().split("T")[0];
  registerVisible.value = true;
};

const submitRegister = async () => {
  await request.post("/api/patients/registrations", regForm);
  ElMessage.success("挂号成功，请引导患者前往诊室");
  registerVisible.value = false;
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>
