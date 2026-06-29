<template>
  <div class="department-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <div>
          <span class="title">科室管理</span>
          <p class="subtitle">维护挂号、排班和医生档案使用的科室基础资料</p>
        </div>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增科室</el-button>
      </div>

      <div class="filter-box">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="关键字">
            <el-input v-model.trim="queryParams.keyword" placeholder="科室名称、编码、位置" clearable style="width: 220px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="fetchDepartments">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="departments"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column prop="sortNo" label="排序" align="center" width="80" />
        <el-table-column prop="name" label="科室名称" align="center" width="150" />
        <el-table-column prop="code" label="科室编码" align="center" width="150" />
        <el-table-column prop="location" label="位置" align="center" width="150" />
        <el-table-column prop="description" label="说明" header-align="center" min-width="220" />
        <el-table-column label="状态" align="center" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
              @change="toggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该科室？已有医生使用的科室不能删除。" @confirm="deleteDepartment(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="科室名称" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item label="科室编码">
          <el-input v-model.trim="form.code" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model.trim="form.location" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model.trim="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitDepartment">保存</el-button>
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
const dialogVisible = ref(false);
const formRef = ref();
const departments = ref([]);

const queryParams = reactive({
  keyword: "",
});

const form = reactive({
  id: null,
  name: "",
  code: "",
  location: "",
  description: "",
  sortNo: 100,
  enabled: 1,
});

const rules = {
  name: [{ required: true, message: "请输入科室名称", trigger: "blur" }],
};

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: "",
    code: "",
    location: "",
    description: "",
    sortNo: 100,
    enabled: 1,
  });
};

const fillForm = (row) => {
  Object.assign(form, {
    id: row.id,
    name: row.name || "",
    code: row.code || "",
    location: row.location || "",
    description: row.description || "",
    sortNo: row.sortNo ?? 100,
    enabled: row.enabled ?? 1,
  });
};

const fetchDepartments = async () => {
  loading.value = true;
  try {
    departments.value = await request.get("/api/departments", {
      params: {
        keyword: queryParams.keyword || undefined,
      },
    });
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  queryParams.keyword = "";
  fetchDepartments();
};

const openDialog = (row) => {
  if (row) {
    fillForm(row);
  } else {
    resetForm();
  }
  dialogVisible.value = true;
};

const submitDepartment = async () => {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await request.put(`/api/departments/${form.id}`, { ...form });
    } else {
      await request.post("/api/departments", { ...form });
    }
    ElMessage.success("科室已保存");
    dialogVisible.value = false;
    fetchDepartments();
  } finally {
    saving.value = false;
  }
};

const toggleEnabled = async (row) => {
  await request.post(`/api/departments/${row.id}/enabled`, null, {
    params: { enabled: row.enabled === 1 },
  });
  ElMessage.success(row.enabled === 1 ? "科室已启用" : "科室已停用");
  fetchDepartments();
};

const deleteDepartment = async (id) => {
  await request.delete(`/api/departments/${id}`);
  ElMessage.success("科室已删除");
  fetchDepartments();
};

onMounted(fetchDepartments);
</script>

<style scoped>
.department-container {
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
</style>
