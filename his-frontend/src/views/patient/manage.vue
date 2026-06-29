<template>
  <div class="patient-manage-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <div>
          <span class="title">患者信息管理</span>
          <p class="subtitle">维护全院患者基础档案</p>
        </div>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增患者</el-button>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="关键字">
            <el-input
              v-model.trim="queryParams.keyword"
              placeholder="姓名、编号、身份证号"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="patients"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column prop="patientNo" label="患者编号" align="center" width="160" />
        <el-table-column prop="name" label="姓名" align="center" width="110" />
        <el-table-column prop="gender" label="性别" align="center" width="80" />
        <el-table-column prop="birthday" label="出生日期" align="center" width="120" />
        <el-table-column prop="phone" label="联系电话" align="center" width="140" />
        <el-table-column prop="idCard" label="身份证号" align="center" min-width="190" />
        <el-table-column prop="balance" label="余额" align="center" width="100">
          <template #default="{ row }">¥{{ Number(row.balance || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="currentStatus" label="当前状态" align="center" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.currentStatus)" effect="light">{{ row.currentStatus || "正常" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该患者档案？" @confirm="deletePatient(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :disabled="loading"
          @current-change="fetchPatients"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑患者档案' : '新增患者档案'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model.trim="form.idCard">
            <template #append>
              <el-button :loading="idCardParsing" :disabled="!form.idCard || form.idCard.length !== 18" @click="parseIdCard">
                识别
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="未知">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model.trim="form.phone" />
        </el-form-item>
        <el-form-item label="就诊状态">
          <el-select v-model="form.currentStatus" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户余额">
          <el-input-number v-model="form.balance" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPatient">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";

const loading = ref(false);
const saving = ref(false);
const idCardParsing = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const patients = ref([]);
const total = ref(0);
const statusOptions = ["正常", "待就诊", "待缴费", "待诊", "就诊中", "已完成"];

const queryParams = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

const form = reactive({
  id: null,
  patientNo: "",
  name: "",
  gender: "未知",
  birthday: "",
  phone: "",
  idCard: "",
  balance: 0,
  currentStatus: "正常",
});

const rules = {
  name: [{ required: true, message: "请输入患者姓名", trigger: "blur" }],
  idCard: [{ required: true, message: "请输入身份证号", trigger: "blur" }],
};

const resetForm = () => {
  Object.assign(form, {
    id: null,
    patientNo: "",
    name: "",
    gender: "未知",
    birthday: "",
    phone: "",
    idCard: "",
    balance: 0,
    currentStatus: "正常",
  });
};

const fillForm = (patient) => {
  Object.assign(form, {
    id: patient.id,
    patientNo: patient.patientNo || "",
    name: patient.name || "",
    gender: patient.gender || "未知",
    birthday: patient.birthday || "",
    phone: patient.phone || "",
    idCard: patient.idCard || "",
    balance: Number(patient.balance || 0),
    currentStatus: patient.currentStatus || "正常",
  });
};

const fetchPatients = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/patients", {
      params: {
        keyword: queryParams.keyword || undefined,
        page: queryParams.page,
        size: queryParams.size,
      },
    });
    patients.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.page = 1;
  fetchPatients();
};

const resetQuery = () => {
  queryParams.keyword = "";
  queryParams.page = 1;
  fetchPatients();
};

const handlePageSizeChange = (size) => {
  queryParams.size = size;
  queryParams.page = 1;
  fetchPatients();
};

const openDialog = (row) => {
  if (row) {
    fillForm(row);
  } else {
    resetForm();
  }
  dialogVisible.value = true;
};

const parseIdCard = async () => {
  if (!form.idCard || form.idCard.length !== 18) {
    return ElMessage.warning("请输入 18 位身份证号");
  }
  idCardParsing.value = true;
  try {
    const data = await request.get("/api/patients/idcard/parse", {
      params: { card: form.idCard },
    });
    form.birthday = data.birthday || form.birthday;
    form.gender = data.gender || form.gender;
    ElMessage.success("身份证信息已识别");
  } finally {
    idCardParsing.value = false;
  }
};

const submitPatient = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = {
      patientNo: form.patientNo || undefined,
      name: form.name,
      gender: form.gender,
      birthday: form.birthday || undefined,
      phone: form.phone,
      idCard: form.idCard,
      balance: form.balance,
      currentStatus: form.currentStatus,
    };
    if (form.id) {
      await request.put(`/api/patients/${form.id}`, payload);
    } else {
      await request.post("/api/patients", payload);
    }
    ElMessage.success("患者档案已保存");
    dialogVisible.value = false;
    fetchPatients();
  } finally {
    saving.value = false;
  }
};

const deletePatient = async (id) => {
  await request.delete(`/api/patients/${id}`);
  ElMessage.success("患者档案已删除");
  fetchPatients();
};

const statusType = (status) => {
  if (status === "正常" || status === "已完成") return "success";
  if (status === "待缴费" || status === "待就诊" || status === "待诊") return "warning";
  if (status === "就诊中") return "primary";
  return "info";
};

onMounted(fetchPatients);
</script>

<style scoped>
.patient-manage-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: #f0f2f5;
}

.main-card {
  min-height: 720px;
  border: none;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.filter-box {
  margin-bottom: 20px;
}

.pagination-box {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
