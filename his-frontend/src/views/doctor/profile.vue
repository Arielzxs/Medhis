<template>
  <div class="profile-container">
    <el-card shadow="never" class="profile-card">
      <div class="header-action">
        <div>
          <span class="title">医生档案管理</span>
          <p class="subtitle">{{ isAdmin ? "维护所有医生的基础资料与在诊状态" : "维护个人基础资料" }}</p>
        </div>
        <el-button v-if="isAdmin" type="primary" icon="Plus" @click="openDialog()">新增档案</el-button>
      </div>

      <template v-if="isAdmin">
        <div class="filter-box">
          <el-form :inline="true" :model="queryParams">
            <el-form-item label="科室">
              <el-select v-model="queryParams.department" placeholder="全部科室" clearable style="width: 160px">
                <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.attendanceStatus" placeholder="全部状态" clearable style="width: 140px">
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="queryParams.keyword" placeholder="姓名或专长" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="fetchProfiles">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-table
          :data="pagedProfiles"
          border
          stripe
          v-loading="loading"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        >
          <el-table-column prop="name" label="医生姓名" align="center" width="120" />
          <el-table-column prop="department" label="所属科室" align="center" width="140" />
          <el-table-column prop="title" label="职称" align="center" width="130" />
          <el-table-column prop="specialty" label="专业特长" header-align="center" min-width="220" />
          <el-table-column prop="attendanceStatus" label="状态" align="center" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.attendanceStatus)" effect="light">
                {{ row.attendanceStatus || "待完善" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="110" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="profilePage.page"
            v-model:page-size="profilePage.size"
            :page-sizes="[10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="profiles.length"
          />
        </div>
      </template>

      <template v-else>
        <el-skeleton v-if="loading" :rows="6" animated />
        <el-form v-else ref="formRef" :model="form" :rules="doctorRules" label-width="96px" class="self-form">
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item label="姓名" prop="name">
                <el-input v-model.trim="form.name" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="当前状态">
                <el-tag :type="statusType(form.attendanceStatus)" effect="light">
                  {{ form.attendanceStatus || "待完善" }}
                </el-tag>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="所属科室" prop="department">
                <el-select v-model="form.department" filterable allow-create default-first-option style="width: 100%">
                  <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="职称" prop="title">
                <el-select v-model="form.title" style="width: 100%">
                  <el-option v-for="item in titleOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="专业特长" prop="specialty">
                <el-input v-model.trim="form.specialty" type="textarea" :rows="4" />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="submitSelfProfile">保存个人信息</el-button>
          </div>
        </el-form>
      </template>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑医生档案' : '新增医生档案'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="adminRules" label-width="96px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item label="关联账号ID">
          <el-input-number v-model="form.userId" :min="1" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="所属科室" prop="department">
          <el-select v-model="form.department" filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-select v-model="form.title" style="width: 100%">
            <el-option v-for="item in titleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业特长" prop="specialty">
          <el-input v-model.trim="form.specialty" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="attendanceStatus">
          <el-radio-group v-model="form.attendanceStatus">
            <el-radio v-for="item in statusOptions" :key="item" :label="item" />
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdminProfile">保存档案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "../../utils/request";
import { useUserStore } from "../../store/user";
import { fetchDepartmentOptions } from "../../utils/departments";

const userStore = useUserStore();
const isAdmin = computed(() => userStore.roles.includes("ADMIN"));

const departments = ref(["待分配"]);
const titleOptions = ["医师", "主治医师", "副主任医师", "主任医师"];
const statusOptions = ["待完善", "在诊", "在岗", "休息", "停诊", "离职"];

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const formRef = ref();
const profiles = ref([]);
const profilePage = reactive({
  page: 1,
  size: 10,
});

const queryParams = reactive({
  department: "",
  attendanceStatus: "",
  keyword: "",
});

const pagedProfiles = computed(() => {
  const start = (profilePage.page - 1) * profilePage.size;
  return profiles.value.slice(start, start + profilePage.size);
});

const form = reactive({
  id: null,
  userId: null,
  name: "",
  department: "待分配",
  title: "医师",
  specialty: "未填写",
  attendanceStatus: "在诊",
});

const baseRules = {
  name: [{ required: true, message: "请输入医生姓名", trigger: "blur" }],
  department: [{ required: true, message: "请选择所属科室", trigger: "change" }],
  title: [{ required: true, message: "请选择职称", trigger: "change" }],
  specialty: [{ required: true, message: "请输入专业特长", trigger: "blur" }],
};
const adminRules = {
  ...baseRules,
  attendanceStatus: [{ required: true, message: "请选择状态", trigger: "change" }],
};
const doctorRules = baseRules;

const resetForm = () => {
  Object.assign(form, {
    id: null,
    userId: null,
    name: "",
    department: "待分配",
    title: "医师",
    specialty: "未填写",
    attendanceStatus: "在诊",
  });
};

const fillForm = (profile) => {
  Object.assign(form, {
    id: profile.id,
    userId: profile.userId || null,
    name: profile.name || "",
    department: profile.department || "待分配",
    title: profile.title || "医师",
    specialty: profile.specialty || "未填写",
    attendanceStatus: profile.attendanceStatus || "待完善",
  });
};

const fetchProfiles = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/doctors/profiles", {
      params: {
        department: queryParams.department || undefined,
        attendanceStatus: queryParams.attendanceStatus || undefined,
        keyword: queryParams.keyword || undefined,
      },
    });
    profiles.value = data || [];
    profilePage.page = 1;
  } finally {
    loading.value = false;
  }
};

const fetchMyProfile = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/doctors/me/profile");
    fillForm(data);
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  queryParams.department = "";
  queryParams.attendanceStatus = "";
  queryParams.keyword = "";
  profilePage.page = 1;
  fetchProfiles();
};

const openDialog = (row) => {
  if (row) {
    fillForm(row);
  } else {
    resetForm();
  }
  dialogVisible.value = true;
};

const submitAdminProfile = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await request.post("/api/doctors", { ...form });
    ElMessage.success("医生档案已保存");
    dialogVisible.value = false;
    fetchProfiles();
  } finally {
    saving.value = false;
  }
};

const submitSelfProfile = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const data = await request.put("/api/doctors/me/profile", {
      name: form.name,
      department: form.department,
      title: form.title,
      specialty: form.specialty,
    });
    fillForm(data);
    ElMessage.success("个人信息已保存");
  } finally {
    saving.value = false;
  }
};

const statusType = (status) => {
  if (status === "在岗" || status === "在诊") return "success";
  if (status === "待完善") return "warning";
  if (status === "休息" || status === "停诊") return "info";
  if (status === "离职") return "danger";
  return "info";
};

onMounted(() => {
  fetchDepartmentOptions({ includePending: true }).then((data) => {
    departments.value = data;
  });
  if (isAdmin.value) {
    fetchProfiles();
  } else {
    fetchMyProfile();
  }
});
</script>

<style scoped>
.profile-container {
  min-height: calc(100vh - 60px);
  padding: 20px;
  background: #f0f2f5;
}

.profile-card {
  min-height: 720px;
  border: none;
  border-radius: 8px;
}

.header-action {
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

.self-form {
  max-width: 920px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
