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
                <el-button type="success" icon="Plus" @click="openCreateAccountDialog"
                  >新增职工账号</el-button
                >
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
                  @change="handleAccountStatusChange(scope.row)"
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
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="openRoleDialog(scope.row)"
                  >修改角色</el-button
                >
                <el-button
                  type="warning"
                  link
                  size="small"
                  @click="openResetPasswordDialog(scope.row)"
                  >重置密码</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== 2. 医生档案管理 ==================== -->
        <el-tab-pane label="医生档案管理" name="doctorProfiles">
          <div class="filter-box">
            <el-form :inline="true" :model="doctorProfileQuery">
              <el-form-item label="科室">
                <el-select
                  v-model="doctorProfileQuery.department"
                  placeholder="全部科室"
                  clearable
                  style="width: 160px"
                >
                  <el-option
                    v-for="department in departments"
                    :key="department"
                    :label="department"
                    :value="department"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="在岗状态">
                <el-select
                  v-model="doctorProfileQuery.attendanceStatus"
                  placeholder="全部状态"
                  clearable
                  style="width: 140px"
                >
                  <el-option label="待完善" value="待完善" />
                  <el-option label="在岗" value="在岗" />
                  <el-option label="停诊" value="停诊" />
                  <el-option label="离职" value="离职" />
                </el-select>
              </el-form-item>
              <el-form-item label="关键字">
                <el-input
                  v-model="doctorProfileQuery.keyword"
                  placeholder="姓名或专长"
                  clearable
                  style="width: 180px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="fetchDoctorProfiles"
                  >查询</el-button
                >
                <el-button type="success" icon="Plus" @click="openDoctorProfileDialog()"
                  >新增医生档案</el-button
                >
              </el-form-item>
            </el-form>
          </div>

          <el-table
            :data="pagedDoctorProfiles"
            border
            stripe
            style="width: 100%"
            v-loading="loadingDoctorProfiles"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column prop="name" label="医生姓名" align="center" width="120" />
            <el-table-column prop="department" label="所属科室" align="center" width="140" />
            <el-table-column prop="title" label="职称" align="center" width="120" />
            <el-table-column prop="specialty" label="专业特长" header-align="center" />
            <el-table-column label="关联账号" align="center" width="140">
              <template #default="{ row }">
                <template v-if="row.userId && accountMap[row.userId]">
                  <el-tag type="primary" effect="light" size="small">
                    {{ accountMap[row.userId].empNo }} {{ accountMap[row.userId].name }}
                  </el-tag>
                </template>
                <span v-else class="text-muted">未关联</span>
              </template>
            </el-table-column>
            <el-table-column prop="attendanceStatus" label="在岗状态" align="center" width="120">
              <template #default="{ row }">
                <el-tag :type="doctorStatusType(row.attendanceStatus)" effect="light">
                  {{ row.attendanceStatus || "待完善" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDoctorDetail(row)"
                  >详情</el-button
                >
                <el-button type="primary" link size="small" @click="openDoctorProfileDialog(row)"
                  >编辑</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination
              v-model:current-page="doctorProfilePage.page"
              v-model:page-size="doctorProfilePage.size"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="doctorProfiles.length"
            />
          </div>
        </el-tab-pane>

        <!-- ==================== 3. 角色权限矩阵配置 ==================== -->
        <el-tab-pane label="角色权限矩阵配置" name="roles">
          <el-row :gutter="20" class="role-matrix-container">
            <el-col :span="8">
              <h4 class="section-title">1. 选择系统角色</h4>
              <div class="role-list">
                <div
                  v-for="role in roleDefinitions"
                  :key="role.code"
                  class="role-item"
                  :class="{ active: activeRole === role.code }"
                  @click="selectRole(role.code)"
                >
                  <div class="role-name">{{ role.label }}</div>
                  <div class="role-desc">{{ role.description }}</div>
                  <div class="role-count">
                    已配置 {{ roleMatrix[role.code]?.length || 0 }} 项权限
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="16">
              <div class="permission-header">
                <div>
                  <h4 class="section-title">2. 配置权限矩阵</h4>
                  <p class="permission-subtitle">
                    当前角色：{{ activeRoleInfo?.label || "未选择" }}
                  </p>
                </div>
                <div>
                  <el-button @click="resetToDefault" :disabled="!activeRole"
                    >恢复默认</el-button
                  >
                  <el-button
                    type="primary"
                    :loading="savingPermissions"
                    :disabled="!activeRole"
                    @click="savePermissions"
                    >保存配置</el-button
                  >
                </div>
              </div>
              <el-card shadow="never" class="permission-card" v-loading="loadingPermissions">
                <el-empty v-if="!activeRole" description="请选择角色" />
                <div v-else>
                  <el-alert
                    title="勾选该角色允许访问的菜单和业务操作"
                    type="info"
                    :description="`保存后会写入角色权限矩阵表。当前已选择 ${checkedPermissionCount} / ${permissionLeafCount} 项权限。`"
                    show-icon
                    :closable="false"
                  />
                  <el-tree
                    ref="permissionTreeRef"
                    class="permission-tree"
                    :data="permissionTree"
                    show-checkbox
                    node-key="id"
                    default-expand-all
                    :props="{ label: 'label', children: 'children' }"
                    @check="syncCheckedPermissions"
                  >
                    <template #default="{ data }">
                      <div class="permission-node">
                        <span>{{ data.label }}</span>
                        <el-tag v-if="data.type" size="small" effect="plain">{{
                          data.type
                        }}</el-tag>
                      </div>
                    </template>
                  </el-tree>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- ==================== 4. 系统安全审计日志 ==================== -->
        <el-tab-pane label="系统安全审计日志" name="logs">
          <div class="log-header">
            <span class="log-desc"
              >实时监控 Spring Security 全局拦截与敏感操作审计日志</span
            >
            <el-button type="danger" plain size="small" @click="clearLogs"
              >清空日志</el-button
            >
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
          <div class="table-pagination">
            <el-pagination
              v-model:current-page="logPage.page"
              v-model:page-size="logPage.size"
              :page-sizes="[10, 20, 50]"
              background
              layout="total, sizes, prev, pager, next"
              :total="logPage.total"
              @current-change="fetchLogs"
              @size-change="handleLogPageSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="accountDialogVisible"
      title="新增职工账号"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="accountFormRef"
        :model="accountForm"
        :rules="accountRules"
        label-width="96px"
      >
        <el-form-item label="职工工号" prop="username">
          <el-input v-model.trim="accountForm.username" placeholder="请输入登录工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="accountForm.name" placeholder="请输入职工姓名" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input
            v-model="accountForm.password"
            placeholder="请输入初始密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="业务角色" prop="role">
          <el-select v-model="accountForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleDefinitions"
              :key="role.code"
              :label="role.label"
              :value="role.code"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAccount" @click="submitAccount"
          >创建账号</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="roleDialogVisible"
      title="修改职工角色"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="96px">
        <el-form-item label="职工工号">
          <span>{{ currentAccount?.empNo }}</span>
        </el-form-item>
        <el-form-item label="姓名">
          <span>{{ currentAccount?.name }}</span>
        </el-form-item>
        <el-form-item label="系统角色">
          <el-select
            v-model="roleForm.roles"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleDefinitions"
              :key="role.code"
              :label="role.label"
              :value="role.code"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="submitRoleChange"
          >保存角色</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="doctorProfileDialogVisible"
      :title="doctorProfileForm.id ? '编辑医生档案' : '新增医生档案'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="doctorProfileFormRef"
        :model="doctorProfileForm"
        :rules="doctorProfileRules"
        label-width="108px"
      >
        <el-form-item label="医生姓名" prop="name">
          <el-input v-model.trim="doctorProfileForm.name" placeholder="请输入医生姓名" />
        </el-form-item>
        <el-form-item label="关联职工账号">
          <el-select
            v-model="doctorProfileForm.userId"
            placeholder="选择关联的登录账号（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in doctorAccountCandidates"
              :key="user.id"
              :label="`${user.username} — ${user.name}`"
              :value="user.id"
              :disabled="user._alreadyLinked"
            >
              <span>{{ user.username }} — {{ user.name }}</span>
              <el-tag v-if="user._alreadyLinked" type="warning" size="small" style="margin-left: 8px">已被关联</el-tag>
            </el-option>
          </el-select>
          <span class="form-tip">不选则医生暂无登录权限，后续可在编辑时补充</span>
        </el-form-item>
        <el-form-item label="所属科室" prop="department">
          <el-select
            v-model="doctorProfileForm.department"
            placeholder="请选择科室"
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="dep in departments"
              :key="dep"
              :label="dep"
              :value="dep"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-select v-model="doctorProfileForm.title" placeholder="请选择职称" style="width: 100%">
            <el-option label="医师" value="医师" />
            <el-option label="主治医师" value="主治医师" />
            <el-option label="副主任医师" value="副主任医师" />
            <el-option label="主任医师" value="主任医师" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业特长" prop="specialty">
          <el-input
            v-model.trim="doctorProfileForm.specialty"
            type="textarea"
            :rows="3"
            placeholder="请输入专业特长"
          />
        </el-form-item>
        <el-form-item label="在岗状态" prop="attendanceStatus">
          <el-radio-group v-model="doctorProfileForm.attendanceStatus">
            <el-radio label="待完善" />
            <el-radio label="在岗" />
            <el-radio label="停诊" />
            <el-radio label="离职" />
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="doctorProfileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingDoctorProfile" @click="submitDoctorProfile"
          >保存档案</el-button
        >
      </template>
    </el-dialog>

    <!-- ==================== 医生详情抽屉 ==================== -->
    <el-drawer
      v-model="doctorDetailVisible"
      :title="doctorDetail?.name || '医生详情'"
      size="520px"
      destroy-on-close
    >
      <template v-if="doctorDetail">
        <el-descriptions :column="1" border style="margin-bottom: 20px">
          <el-descriptions-item label="医生姓名">
            {{ doctorDetail.name }}
          </el-descriptions-item>
          <el-descriptions-item label="所属科室">
            {{ doctorDetail.department || "未填写" }}
          </el-descriptions-item>
          <el-descriptions-item label="职称">
            {{ doctorDetail.title || "未填写" }}
          </el-descriptions-item>
          <el-descriptions-item label="在岗状态">
            <el-tag :type="doctorStatusType(doctorDetail.attendanceStatus)" effect="light">
              {{ doctorDetail.attendanceStatus || "待完善" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="专业特长">
            {{ doctorDetail.specialty || "未填写" }}
          </el-descriptions-item>
          <el-descriptions-item label="关联登录账号">
            <template v-if="doctorDetail.userId && accountMap[doctorDetail.userId]">
              <el-tag type="primary" effect="light">
                {{ accountMap[doctorDetail.userId].empNo }} — {{ accountMap[doctorDetail.userId].name }}
              </el-tag>
              <span style="margin-left: 4px; font-size: 12px; color: #909399">
                (角色: {{ accountMap[doctorDetail.userId].role }})
              </span>
            </template>
            <span v-else class="text-muted">未关联 — 该医生暂无系统登录权限</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">排班概况</el-divider>
        <div v-loading="loadingDoctorSchedules" style="min-height: 80px">
          <el-empty v-if="!loadingDoctorSchedules && !doctorSchedules.length" description="暂无排班记录" />
          <el-timeline v-else-if="!loadingDoctorSchedules">
            <el-timeline-item
              v-for="schedule in doctorSchedules.slice(0, 10)"
              :key="schedule.id"
              :timestamp="schedule.scheduleDate"
              placement="top"
            >
              <el-card shadow="hover" style="margin-bottom: 8px">
                <div>{{ schedule.shift || '全天' }} · {{ schedule.department || doctorDetail.department }}</div>
                <div style="font-size: 12px; color: #909399; margin-top: 4px">
                  号别: {{ schedule.level === 1 ? '专家号' : '普通号' }} |
                  限额: {{ schedule.limit ?? '—' }} |
                  剩余: {{ schedule.remain ?? '—' }} |
                  <el-tag :type="schedule.status === 1 ? 'success' : 'info'" size="small" effect="plain">
                    {{ schedule.status === 1 ? '正常' : '停诊' }}
                  </el-tag>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <div v-if="doctorSchedules.length > 10" style="text-align: center; margin-top: 8px; color: #909399; font-size: 12px">
            仅展示最近 10 条，共 {{ doctorSchedules.length }} 条排班记录
          </div>
        </div>

        <el-divider content-position="left">接诊统计</el-divider>
        <div v-loading="loadingDoctorWorkload" style="min-height: 80px">
          <el-empty v-if="!loadingDoctorWorkload && doctorWorkloadCount === null" description="暂无接诊统计数据" />
          <el-row v-else-if="!loadingDoctorWorkload" :gutter="16">
            <el-col :span="12">
              <el-statistic title="累计接诊人次" :value="doctorWorkloadCount" />
            </el-col>
          </el-row>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="passwordDialogVisible"
      title="重置密码"
      width="460px"
      destroy-on-close
    >
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="重置后请及时通知该职工使用新密码登录。"
        style="margin-bottom: 16px"
      />
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="96px"
      >
        <el-form-item label="职工工号">
          <span>{{ currentAccount?.empNo }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="passwordForm.password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button
          type="warning"
          :loading="savingPassword"
          @click="submitResetPassword"
          >确认重置</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../utils/request";
import { fetchDepartmentOptions } from "../../utils/departments";

const activeTab = ref("accounts");
const loading = ref(false);

// ===== 账户管理数据 =====
const accountQuery = reactive({ keyword: "", role: "" });
const accountList = ref([]);
const accountDialogVisible = ref(false);
const roleDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const savingAccount = ref(false);
const savingRole = ref(false);
const savingPassword = ref(false);
const accountFormRef = ref();
const passwordFormRef = ref();
const currentAccount = ref(null);
const accountForm = reactive({
  username: "",
  name: "",
  password: "123456",
  role: "DOCTOR",
});
const roleForm = reactive({
  roles: [],
});
const passwordForm = reactive({
  password: "",
});
const accountRules = {
  username: [
    { required: true, message: "请输入职工工号", trigger: "blur" },
    { pattern: /^[A-Za-z0-9_-]{3,20}$/, message: "工号需为3-20位字母、数字、下划线或短横线", trigger: "blur" },
  ],
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入初始密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度需为6-32位", trigger: "blur" },
  ],
  role: [{ required: true, message: "请选择业务角色", trigger: "change" }],
};
const passwordRules = {
  password: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度需为6-32位", trigger: "blur" },
  ],
};

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
      id: user.id,
      empNo: user.username,
      name: user.name,
      department: "--",
      role: (user.roles || []).map(roleLabel).join("、") || "未分配",
      roles: [...(user.roles || [])],
      status: user.enabled === "Y" ? 1 : 0,
    }));
  } finally {
    loading.value = false;
  }
};

const resetAccountForm = () => {
  Object.assign(accountForm, {
    username: "",
    name: "",
    password: "123456",
    role: "DOCTOR",
  });
};

const openCreateAccountDialog = () => {
  resetAccountForm();
  accountDialogVisible.value = true;
};

const submitAccount = async () => {
  await accountFormRef.value?.validate();
  savingAccount.value = true;
  try {
    await request.post("/api/auth/users", {
      username: accountForm.username,
      name: accountForm.name,
      password: accountForm.password,
      role: accountForm.role,
    });
    ElMessage.success("职工账号创建成功");
    accountDialogVisible.value = false;
    fetchAccounts();
  } finally {
    savingAccount.value = false;
  }
};

const openRoleDialog = (row) => {
  currentAccount.value = row;
  roleForm.roles = [...(row.roles || [])];
  roleDialogVisible.value = true;
};

const submitRoleChange = async () => {
  if (!roleForm.roles.length) {
    ElMessage.warning("请至少选择一个角色");
    return;
  }
  savingRole.value = true;
  try {
    await request.post(`/api/auth/${currentAccount.value.id}/roles`, roleForm.roles);
    ElMessage.success("角色修改成功");
    roleDialogVisible.value = false;
    fetchAccounts();
  } finally {
    savingRole.value = false;
  }
};

const openResetPasswordDialog = (row) => {
  currentAccount.value = row;
  passwordForm.password = "123456";
  passwordDialogVisible.value = true;
};

const submitResetPassword = async () => {
  await passwordFormRef.value?.validate();
  await ElMessageBox.confirm(
    `确认将 ${currentAccount.value.name}（${currentAccount.value.empNo}）的密码重置吗？`,
    "重置密码确认",
    {
      confirmButtonText: "确认重置",
      cancelButtonText: "取消",
      type: "warning",
    },
  );
  savingPassword.value = true;
  try {
    await request.put(`/api/auth/users/${currentAccount.value.id}/password`, {
      password: passwordForm.password,
    });
    ElMessage.success("密码已重置");
    passwordDialogVisible.value = false;
  } finally {
    savingPassword.value = false;
  }
};

const handleAccountStatusChange = async (row) => {
  const enabled = row.status === 1;
  try {
    await request.put(`/api/auth/users/${row.id}/enabled`, { enabled });
    ElMessage.success(enabled ? "账户已启用" : "账户已停用");
  } catch (error) {
    row.status = enabled ? 0 : 1;
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

// ===== 医生档案管理数据 =====
const doctorProfileQuery = reactive({
  department: "",
  attendanceStatus: "",
  keyword: "",
});
const doctorProfiles = ref([]);
const loadingDoctorProfiles = ref(false);
const savingDoctorProfile = ref(false);
const doctorProfileDialogVisible = ref(false);
const doctorProfileFormRef = ref();
const doctorProfileForm = reactive({
  id: null,
  userId: null,
  name: "",
  department: "",
  title: "医师",
  specialty: "未填写",
  attendanceStatus: "待完善",
});
const doctorProfileRules = {
  name: [{ required: true, message: "请输入医生姓名", trigger: "blur" }],
  department: [{ required: true, message: "请选择医生所属科室", trigger: "change" }],
  title: [{ required: true, message: "请选择医生职称", trigger: "change" }],
  attendanceStatus: [{ required: true, message: "请选择在岗状态", trigger: "change" }],
};

const departments = ref(["待分配"]);
// 所有职工账号映射 { userId: { id, username, name, role } }
const accountMap = ref({});
// 可关联的医生候选账号（角色为 DOCTOR 的职工）
const doctorAccountCandidates = ref([]);

const doctorProfilePage = reactive({ page: 1, size: 10 });
const pagedDoctorProfiles = computed(() => {
  const start = (doctorProfilePage.page - 1) * doctorProfilePage.size;
  return doctorProfiles.value.slice(start, start + doctorProfilePage.size);
});

// ===== 医生详情抽屉数据 =====
const doctorDetailVisible = ref(false);
const doctorDetail = ref(null);
const doctorSchedules = ref([]);
const doctorWorkloadCount = ref(null);
const loadingDoctorSchedules = ref(false);
const loadingDoctorWorkload = ref(false);

const buildAccountMap = async () => {
  try {
    const users = await request.get("/api/auth/users");
    const map = {};
    const doctorCandidates = [];
    (users || []).forEach((user) => {
      const roles = user.roles || [];
      map[user.id] = {
        id: user.id,
        empNo: user.username,
        name: user.name,
        role: roles.map(roleLabel).join("、") || "未分配",
        roles,
      };
      // 筛选角色包含 DOCTOR 的职工作为候选
      if (roles.includes("DOCTOR")) {
        doctorCandidates.push({ ...user, _alreadyLinked: false });
      }
    });
    accountMap.value = map;
    doctorAccountCandidates.value = doctorCandidates;
  } catch {
    // 账号列表加载失败不影响主流程
  }
};

// 标记已被其他档案关联的账号
const markLinkedAccounts = () => {
  const candidates = doctorAccountCandidates.value.map((u) => ({
    ...u,
    _alreadyLinked: false,
  }));
  doctorProfiles.value.forEach((profile) => {
    if (profile.userId) {
      const idx = candidates.findIndex((u) => u.id === profile.userId);
      if (idx >= 0 && profile.id !== doctorProfileForm.id) {
        candidates[idx]._alreadyLinked = true;
      }
    }
  });
  doctorAccountCandidates.value = candidates;
};

const fetchDoctorProfiles = async () => {
  loadingDoctorProfiles.value = true;
  try {
    const data = await request.get("/api/doctors/profiles", {
      params: {
        department: doctorProfileQuery.department || undefined,
        attendanceStatus: doctorProfileQuery.attendanceStatus || undefined,
        keyword: doctorProfileQuery.keyword || undefined,
      },
    });
    doctorProfiles.value = data || [];
    // 从档案数据中提取科室到下拉列表
    const dynamicDepts = new Set(departments.value);
    (data || []).forEach((p) => {
      if (p.department) dynamicDepts.add(p.department);
    });
    departments.value = [...dynamicDepts];
    doctorProfilePage.page = 1;
    // 更新账号关联标记
    markLinkedAccounts();
  } finally {
    loadingDoctorProfiles.value = false;
  }
};

const fetchDepartments = async () => {
  departments.value = await fetchDepartmentOptions({ includePending: true });
};

const resetDoctorProfileForm = () => {
  Object.assign(doctorProfileForm, {
    id: null,
    userId: null,
    name: "",
    department: "待分配",
    title: "医师",
    specialty: "未填写",
    attendanceStatus: "待完善",
  });
};

const openDoctorProfileDialog = (row) => {
  if (row) {
    Object.assign(doctorProfileForm, {
      id: row.id,
      userId: row.userId || null,
      name: row.name,
      department: row.department || "待分配",
      title: row.title || "医师",
      specialty: row.specialty || "未填写",
      attendanceStatus: row.attendanceStatus || "待完善",
    });
  } else {
    resetDoctorProfileForm();
  }
  markLinkedAccounts();
  doctorProfileDialogVisible.value = true;
};

const submitDoctorProfile = async () => {
  await doctorProfileFormRef.value?.validate();
  savingDoctorProfile.value = true;
  try {
    await request.post("/api/doctors", {
      id: doctorProfileForm.id || null,
      userId: doctorProfileForm.userId || null,
      name: doctorProfileForm.name,
      department: doctorProfileForm.department,
      title: doctorProfileForm.title,
      specialty: doctorProfileForm.specialty || "未填写",
      attendanceStatus: doctorProfileForm.attendanceStatus,
    });
    ElMessage.success("医生档案已保存");
    doctorProfileDialogVisible.value = false;
    await fetchDoctorProfiles();
    await buildAccountMap();
  } finally {
    savingDoctorProfile.value = false;
  }
};

// ===== 医生详情 =====
const openDoctorDetail = async (row) => {
  doctorDetail.value = { ...row };
  doctorDetailVisible.value = true;
  // 异步加载排班和工作量，不阻塞抽屉打开
  await Promise.allSettled([
    fetchSchedulesForDoctor(row),
    fetchWorkloadForDoctor(row),
  ]);
};

const fetchSchedulesForDoctor = async (doctor) => {
  loadingDoctorSchedules.value = true;
  try {
    const res = await request.get("/api/doctors/schedules", {
      params: {
        department: doctor.department || undefined,
        doctorName: doctor.name,
        page: 1,
        size: 50,
      },
    });
    doctorSchedules.value = (res?.records || []).filter(
      (s) => s.doctorName === doctor.name,
    );
  } catch {
    doctorSchedules.value = [];
  } finally {
    loadingDoctorSchedules.value = false;
  }
};

const fetchWorkloadForDoctor = async (doctor) => {
  loadingDoctorWorkload.value = true;
  try {
    const map = await request.get("/api/analytics/doctor-workload");
    // map 形如 { "Doctor_ID_1": 5, "Doctor_ID_2": 12 }
    // MedicalRecord.doctorId 可能是 doctorProfile.userId 或 doctorProfile.id，两者都尝试匹配
    const keysToTry = [];
    if (doctor.userId) keysToTry.push(`Doctor_ID_${doctor.userId}`);
    if (doctor.id) keysToTry.push(`Doctor_ID_${doctor.id}`);
    let count = null;
    for (const key of keysToTry) {
      if (map && map[key] !== undefined) {
        count = map[key];
        break;
      }
    }
    doctorWorkloadCount.value = count;
  } catch {
    doctorWorkloadCount.value = null;
  } finally {
    loadingDoctorWorkload.value = false;
  }
};

const doctorStatusType = (status) => {
  if (status === "在岗") return "success";
  if (status === "待完善") return "warning";
  if (status === "停诊") return "info";
  if (status === "离职") return "danger";
  return "info";
};

// ===== 角色权限矩阵数据 =====
const activeRole = ref("ADMIN");
const loadingPermissions = ref(false);
const savingPermissions = ref(false);
const permissionTreeRef = ref();
const roleMatrix = reactive({});
const defaultRoleMatrix = reactive({});
const checkedPermissions = ref([]);

const roleDefinitions = [
  {
    code: "ADMIN",
    label: "系统管理员",
    description: "拥有全院系统配置及底层基础支撑最高权限",
  },
  {
    code: "DOCTOR",
    label: "门诊医生",
    description: "负责排班查看、接诊问诊、电子病历与处方开立",
  },
  {
    code: "REGISTRAR",
    label: "挂号收费员",
    description: "负责门诊挂号、患者就诊追踪与收费协同",
  },
  {
    code: "PHARMACY_ADMIN",
    label: "药房管理员",
    description: "负责处方审核、配药发药与药品库存维护",
  },
  {
    code: "FINANCE",
    label: "财务人员",
    description: "负责费用结算、财务管理与收支统计报表",
  },
];

const permissionTree = [
  {
    id: "dashboard",
    label: "数据看板",
    children: [{ id: "dashboard:view", label: "查看数据看板", type: "菜单" }],
  },
  {
    id: "patient",
    label: "患者管理",
    children: [
      { id: "patient:registration:view", label: "查看门诊挂号", type: "菜单" },
      { id: "patient:registration:create", label: "新增门诊挂号", type: "操作" },
      { id: "patient:tracking:view", label: "查看就诊状态追踪", type: "菜单" },
    ],
  },
  {
    id: "doctor",
    label: "医生工作站",
    children: [
      { id: "doctor:schedule:view", label: "查看人员排班", type: "菜单" },
      { id: "doctor:schedule:manage", label: "新增/编辑/删除排班", type: "操作" },
      { id: "doctor:consultation:view", label: "进入接诊问诊", type: "菜单" },
      { id: "doctor:record:write", label: "书写电子病历", type: "操作" },
      { id: "doctor:prescription:write", label: "开立处方", type: "操作" },
    ],
  },
  {
    id: "pharmacy",
    label: "药房管理",
    children: [
      { id: "pharmacy:prescription:review", label: "处方审核", type: "操作" },
      { id: "pharmacy:dispense", label: "确认发药", type: "操作" },
      { id: "pharmacy:drug:view", label: "查看药品字典与库存", type: "菜单" },
      { id: "pharmacy:drug:manage", label: "新增/编辑/入库药品", type: "操作" },
    ],
  },
  {
    id: "finance",
    label: "财务管理",
    children: [
      { id: "finance:billing:view", label: "查看收费记录", type: "菜单" },
      { id: "finance:billing:manage", label: "处理收费结算", type: "操作" },
    ],
  },
  {
    id: "analytics",
    label: "统计分析",
    children: [
      { id: "analytics:workload:view", label: "查看工作量统计", type: "报表" },
      { id: "analytics:revenue:view", label: "查看财务收支报表", type: "报表" },
      { id: "analytics:drug:view", label: "查看药品消耗排行", type: "报表" },
    ],
  },
  {
    id: "system",
    label: "权限与基础支撑",
    children: [
      { id: "system:user:manage", label: "维护职工账户", type: "操作" },
      { id: "system:role:manage", label: "配置角色权限矩阵", type: "操作" },
      { id: "system:audit:view", label: "查看系统审计日志", type: "菜单" },
    ],
  },
];

const permissionLeafCodes = permissionTree.flatMap((group) =>
  group.children.map((item) => item.id),
);
const permissionLeafCount = permissionLeafCodes.length;
const checkedPermissionCount = computed(() => checkedPermissions.value.length);
const activeRoleInfo = computed(() =>
  roleDefinitions.find((role) => role.code === activeRole.value),
);

const setTreeCheckedKeys = async (keys) => {
  checkedPermissions.value = [...keys];
  await nextTick();
  permissionTreeRef.value?.setCheckedKeys([...keys]);
};

const fetchRoleMatrix = async () => {
  loadingPermissions.value = true;
  try {
    const [matrix, defaults] = await Promise.all([
      request.get("/api/auth/role-permissions"),
      request.get("/api/auth/role-permissions/defaults"),
    ]);
    roleDefinitions.forEach((role) => {
      roleMatrix[role.code] = [...(matrix?.[role.code] || [])];
      defaultRoleMatrix[role.code] = [...(defaults?.[role.code] || [])];
    });
    await selectRole(activeRole.value);
  } finally {
    loadingPermissions.value = false;
  }
};

const selectRole = async (roleCode) => {
  activeRole.value = roleCode;
  await setTreeCheckedKeys(roleMatrix[roleCode] || []);
};

const syncCheckedPermissions = () => {
  checkedPermissions.value = permissionTreeRef.value?.getCheckedKeys(true) || [];
};

const savePermissions = async () => {
  syncCheckedPermissions();
  savingPermissions.value = true;
  try {
    await request.put(
      `/api/auth/role-permissions/${activeRole.value}`,
      checkedPermissions.value,
    );
    roleMatrix[activeRole.value] = [...checkedPermissions.value];
    ElMessage.success("角色权限矩阵已保存");
  } finally {
    savingPermissions.value = false;
  }
};

const resetToDefault = async () => {
  await setTreeCheckedKeys(defaultRoleMatrix[activeRole.value] || []);
  ElMessage.info("已恢复为默认权限，点击保存后生效");
};

// ===== 审计日志数据 =====
const auditLogs = ref([]);
const logPage = reactive({ page: 1, size: 10, total: 0 });

const fetchLogs = async () => {
  const res = await request.get("/api/audit/logs", {
    params: { page: logPage.page, size: logPage.size },
  });
  auditLogs.value = (res.records || []).map((log) => ({
    time: log.time,
    account: log.username || "--",
    ip: "--",
    module: log.operation || "--",
    description: log.detail || "",
    status: "放行成功",
  }));
  logPage.total = res.total || 0;
};

const handleLogPageSizeChange = () => {
  logPage.page = 1;
  fetchLogs();
};

const clearLogs = async () => {
  await ElMessageBox.confirm(
    "确认清空系统安全审计日志吗？该操作会保留一条清空记录用于追溯。",
    "清空日志确认",
    {
      confirmButtonText: "确认清空",
      cancelButtonText: "取消",
      type: "warning",
    },
  );
  await request.delete("/api/audit/logs");
  ElMessage.success("日志已清空");
  fetchLogs();
};

onMounted(() => {
  fetchDepartments();
  fetchAccounts();
  fetchRoleMatrix();
  fetchLogs();
  buildAccountMap();
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

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
.role-count {
  margin-top: 8px;
  font-size: 12px;
  color: #409eff;
}
.permission-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}
.permission-subtitle {
  margin: -8px 0 0;
  font-size: 13px;
  color: #909399;
}
.permission-card {
  min-height: 450px;
  background-color: #fafafa;
}
.permission-tree {
  margin-top: 16px;
  padding: 8px 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.permission-node {
  display: flex;
  align-items: center;
  gap: 8px;
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

.form-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
