<!-- src/views/system/index.vue -->
<template>
  <div class="system-container">
    <el-card shadow="never" class="main-card">
      <div class="page-header">
        <h3 class="title">系统基础支撑与权限安全中心 (RBAC)</h3>
        <el-tag type="danger" effect="dark">安全合规环境</el-tag>
      </div>

      <el-tabs v-model="activeTab" class="system-tabs">
        <!-- ==================== 1. 医院职工账户管理 ==================== -->
        <el-tab-pane label="医院职工账户管理" name="accounts">
          <div class="filter-box">
            <el-form :inline="true" :model="accountQuery">
              <el-form-item label="工号/姓名">
                <el-input
                  v-model="accountQuery.keyword"
                  placeholder="请输入职工工号或姓名"
                  clearable
                  style="width: 200px"
                />
              </el-form-item>
              <el-form-item label="业务角色">
                <el-select
                  v-model="accountQuery.role"
                  placeholder="全部角色"
                  clearable
                  style="width: 150px"
                >
                  <el-option label="管理员" value="管理员" />
                  <el-option label="门诊医生" value="门诊医生" />
                  <el-option label="挂号员" value="挂号员" />
                  <el-option label="药房管理员" value="药房管理员" />
                  <el-option label="财务人员" value="财务人员" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="fetchAccounts"
                  >查询</el-button
                >
                <el-button type="success" icon="Plus">新增职工账号</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="accountList"
            border
            stripe
            style="width: 100%"
            v-loading="loading"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              prop="empNo"
              label="职工工号"
              align="center"
              width="120"
            />
            <el-table-column
              prop="name"
              label="姓名"
              align="center"
              width="120"
            />
            <el-table-column
              prop="department"
              label="所属科室/部门"
              align="center"
            />
            <el-table-column
              prop="role"
              label="当前系统角色"
              align="center"
              width="150"
            >
              <template #default="scope">
                <el-tag :type="getRoleType(scope.row.role)" effect="light">{{
                  scope.row.role
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              label="账户状态"
              align="center"
              width="120"
            >
              <template #default="scope">
                <el-switch
                  v-model="scope.row.status"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="启用"
                  inactive-text="停用"
                  inline-prompt
                />
              </template>
            </el-table-column>
            <el-table-column
              label="安全操作"
              align="center"
              width="200"
              fixed="right"
            >
              <template #default="scope">
                <el-button type="primary" link size="small">修改角色</el-button>
                <el-button type="warning" link size="small">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== 2. 角色权限矩阵配置 ==================== -->
        <el-tab-pane label="角色权限矩阵配置" name="roles">
          <el-row :gutter="20" class="role-matrix-container">
            <el-col :span="8">
              <h4 class="section-title">1. 选择系统角色</h4>
              <div class="role-list">
                <div
                  class="role-item"
                  :class="{ active: activeRole === 'admin' }"
                  @click="activeRole = 'admin'"
                >
                  <div class="role-name">系统管理员</div>
                  <div class="role-desc">
                    拥有全院系统配置及底层基础支撑最高权限
                  </div>
                </div>
                <div
                  class="role-item"
                  :class="{ active: activeRole === 'doctor' }"
                  @click="activeRole = 'doctor'"
                >
                  <div class="role-name">门诊医生</div>
                  <div class="role-desc">
                    主要负责挂号状态追踪、接诊问诊、电子病历处方开立
                  </div>
                </div>
                <div
                  class="role-item"
                  :class="{ active: activeRole === 'registrar' }"
                  @click="activeRole = 'registrar'"
                >
                  <div class="role-name">挂号收费员</div>
                  <div class="role-desc">
                    负责窗口门诊挂号、就诊追踪以及财务缴费结算
                  </div>
                </div>
                <div
                  class="role-item"
                  :class="{ active: activeRole === 'pharmacist' }"
                  @click="activeRole = 'pharmacist'"
                >
                  <div class="role-name">药房管理员</div>
                  <div class="role-desc">
                    负责配药发药流程管控与库房药品盘点入库
                  </div>
                </div>
                <div
                  class="role-item"
                  :class="{ active: activeRole === 'manager' }"
                  @click="activeRole = 'manager'"
                >
                  <div class="role-name">医院运营主管</div>
                  <div class="role-desc">
                    只读调取全院财务、医生、药品的综合统计分析报表
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="16">
              <h4 class="section-title">2. 请在左侧选择一个角色以配置权限</h4>
              <el-card shadow="never" class="permission-card">
                <el-empty v-if="!activeRole" description="请选择角色" />
                <div v-else class="permission-tree-placeholder">
                  <!-- 此处未来可使用 el-tree 渲染对应角色的菜单与接口权限树 -->
                  <el-alert
                    title="权限矩阵配置区"
                    type="info"
                    :description="`当前正在为【${activeRole}】配置细粒度权限（含菜单访问、接口调用权限），该功能待后端 API 对接后激活。`"
                    show-icon
                    :closable="false"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- ==================== 3. 系统安全审计日志 ==================== -->
        <el-tab-pane label="系统安全审计日志" name="logs">
          <div class="log-header">
            <span class="log-desc"
              >实时监控 Spring Security 全局拦截与敏感操作审计日志</span
            >
            <el-button type="danger" plain size="small">清空日志</el-button>
          </div>

          <el-table
            :data="auditLogs"
            border
            stripe
            style="width: 100%"
            v-loading="loading"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column
              prop="time"
              label="操作时间"
              align="center"
              width="160"
            />
            <el-table-column
              prop="account"
              label="操作账户"
              align="center"
              width="150"
            />
            <el-table-column
              prop="ip"
              label="IP 地址"
              align="center"
              width="140"
            />
            <el-table-column
              prop="module"
              label="请求模块"
              align="center"
              width="120"
            />
            <el-table-column
              prop="description"
              label="操作内容描述"
              header-align="center"
              min-width="250"
            />
            <el-table-column
              prop="status"
              label="安全拦截状态"
              align="center"
              width="150"
            >
              <template #default="scope">
                <el-tag
                  :type="scope.row.status === '放行成功' ? 'success' : 'danger'"
                  effect="plain"
                >
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";

const activeTab = ref("accounts");
const loading = ref(false);

// ===== 账户管理数据 =====
const accountQuery = reactive({ keyword: "", role: "" });
const accountList = ref([]);

const roleLabel = (role) => {
  const map = {
    ADMIN: "管理员",
    DOCTOR: "门诊医生",
    REGISTRAR: "挂号员",
    PHARMACY_ADMIN: "药房管理员",
    FINANCE: "财务人员",
  };
  return map[role] || role || "未分配";
};

const roleCode = (label) => {
  const map = {
    管理员: "ADMIN",
    门诊医生: "DOCTOR",
    挂号员: "REGISTRAR",
    药房管理员: "PHARMACY_ADMIN",
    财务人员: "FINANCE",
  };
  return map[label] || label;
};

const fetchAccounts = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/auth/users", {
      params: {
        keyword: accountQuery.keyword || undefined,
        role: accountQuery.role ? roleCode(accountQuery.role) : undefined,
      },
    });
    accountList.value = (res || []).map((user) => ({
      empNo: user.username,
      name: user.name,
      department: "--",
      role: roleLabel(user.roles?.[0]),
      status: user.enabled === "Y" ? 1 : 0,
    }));
  } finally {
    loading.value = false;
  }
};

const getRoleType = (role) => {
  const map = {
    管理员: "danger",
    门诊医生: "primary",
    挂号员: "success",
    药房管理员: "warning",
    财务人员: "success",
  };
  return map[role] || "info";
};

// ===== 角色管理数据 =====
const activeRole = ref("");

// ===== 审计日志数据 =====
const auditLogs = ref([]);

const fetchLogs = async () => {
  const res = await request.get("/api/audit/logs");
  auditLogs.value = (res || []).map((log) => ({
    time: log.time,
    account: log.username || "--",
    ip: "--",
    module: log.operation || "--",
    description: log.detail || "",
    status: "放行成功",
  }));
};

onMounted(() => {
  fetchAccounts();
  fetchLogs();
});
</script>

<style scoped>
.system-container {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.system-tabs {
  margin-top: 10px;
}
.filter-box {
  padding: 15px 0 0 0;
  margin-bottom: 15px;
}

/* 角色矩阵配置样式 */
.role-matrix-container {
  margin-top: 15px;
}
.section-title {
  margin: 0 0 15px 0;
  font-size: 15px;
  color: #303133;
  font-weight: bold;
}
.role-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}
.role-item {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.3s;
}
.role-item:last-child {
  border-bottom: none;
}
.role-item:hover {
  background-color: #f5f7fa;
}
.role-item.active {
  background-color: #f0f7ff;
  border-left: 3px solid #409eff;
}
.role-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
  margin-bottom: 5px;
}
.role-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.permission-card {
  min-height: 450px;
  background-color: #fafafa;
}

/* 审计日志样式 */
.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background-color: #fef0f0;
  border-radius: 4px;
  margin-bottom: 15px;
}
.log-desc {
  font-size: 13px;
  color: #f56c6c;
}
</style>
