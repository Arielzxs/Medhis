<template>
  <div class="app-container">
    <el-card shadow="never">
      <div
        class="filter-container"
        style="margin-bottom: 20px; display: flex; gap: 10px"
      >
        <el-input
          v-model="queryParams.keyword"
          placeholder="患者姓名 / 编号"
          style="width: 250px"
          clearable
          @keyup.enter="handleFilter"
        />
        <el-button type="primary" icon="Search" @click="handleFilter"
          >查询</el-button
        >
        <el-button type="success" icon="Plus" @click="openCreateDialog"
          >新建档案</el-button
        >
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="patientNo" label="患者编号" width="160" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column
          prop="currentStatus"
          label="状态"
          width="120"
          align="center"
        >
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.currentStatus)"
              effect="dark"
            >
              {{ scope.row.currentStatus || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="建档时间" min-width="160" />
        <el-table-column
          label="业务操作"
          width="120"
          fixed="right"
          align="center"
        >
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              icon="Ticket"
              @click="openRegisterDialog(scope.row)"
              >挂号</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="新建患者档案"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="患者姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="患者编号" prop="patientNo">
          <el-input
            v-model="form.patientNo"
            placeholder="系统唯一编号 (可使用身份证号)"
          />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitSave"
          >确 认</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="registerVisible"
      title="门诊现场挂号"
      width="500px"
      destroy-on-close
    >
      <el-form :model="regForm" label-width="90px">
        <el-form-item label="挂号科室">
          <el-select v-model="regForm.department" style="width: 100%">
            <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="排班医生ID">
          <el-input-number
            v-model="regForm.doctorId"
            :min="1"
            style="width: 100%"
            placeholder="请输入医生ID"
          />
        </el-form-item>
        <el-form-item label="挂号金额(元)">
          <el-input-number
            v-model="regForm.fee"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegister"
          >提交并打印凭证</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage } from "element-plus";
import { fetchDepartmentOptions } from "../../utils/departments";

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const departments = ref([]);

const queryParams = reactive({ keyword: "", page: 1, size: 10 });

const dialogVisible = ref(false);
const formRef = ref(null);
const form = reactive({ name: "", patientNo: "", gender: "男", phone: "" });

const registerVisible = ref(false);
const regForm = reactive({
  patientId: null,
  doctorId: 1,
  department: "",
  scheduleDate: "",
  fee: 50.0,
});

// 表单校验规则
const rules = {
  name: [{ required: true, message: "姓名不能为空", trigger: "blur" }],
  patientNo: [{ required: true, message: "编号不能为空", trigger: "blur" }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
};

const getStatusType = (status) => {
  const map = {
    建档: "info",
    待就诊: "warning",
    就诊中: "primary",
    已诊: "success",
  };
  return map[status] || "info";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/patients", { params: queryParams });
    tableData.value = res.records || [];
    total.value = res.total || 0; // 绑定真实总条数
  } finally {
    loading.value = false;
  }
};

const handleFilter = () => {
  queryParams.page = 1;
  fetchData();
};

const openCreateDialog = () => {
  Object.assign(form, { name: "", patientNo: "", gender: "男", phone: "" });
  dialogVisible.value = true;
};

const submitSave = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        await request.post("/api/patients", form);
        ElMessage.success("患者档案建立成功！");
        dialogVisible.value = false;
        fetchData();
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const openRegisterDialog = (row) => {
  regForm.patientId = row.id;
  regForm.scheduleDate = new Date().toISOString().split("T")[0];
  registerVisible.value = true;
};

const submitRegister = async () => {
  await request.post("/api/patients/registrations", regForm);
  ElMessage.success("挂号成功，已生成流水，请引导患者缴费就诊");
  registerVisible.value = false;
  fetchData();
};

onMounted(async () => {
  departments.value = await fetchDepartmentOptions();
  if (!regForm.department) {
    regForm.department = departments.value[0] || "";
  }
  fetchData();
});
</script>
