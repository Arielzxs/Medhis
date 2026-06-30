<!-- src/views/dashboard/index.vue -->
<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎区与快捷操作 -->
    <el-row :gutter="20" class="top-panel">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="welcome-card">
          <div class="welcome-box">
            <el-avatar :size="60" class="user-avatar">
              <el-icon :size="30"><UserFilled /></el-icon>
            </el-avatar>
            <div class="welcome-text">
              <div class="greeting">
                早安，{{ userStore.username }} 管理员！祝您今天工作顺利。
              </div>
              <div class="date-info">
                今天是：{{ currentDate }} | 东软云医院 HIS 系统 - 门诊本部
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="quick-action-card">
          <div class="action-buttons">
            <el-button
              v-if="hasAnyRole(['REGISTRAR'])"
              type="primary"
              size="large"
              @click="$router.push('/patient/registration')"
              >快捷挂号</el-button
            >
            <el-button
              v-if="hasAnyRole(['FINANCE'])"
              type="success"
              size="large"
              @click="$router.push('/finance')"
              >门诊收费</el-button
            >
            <el-button
              v-if="hasAnyRole(['PHARMACY_ADMIN'])"
              type="warning"
              size="large"
              @click="$router.push('/pharmacy')"
              >发药工作台</el-button
            >
            <el-button
              v-if="hasAnyRole(['DOCTOR'])"
              type="primary"
              size="large"
              @click="$router.push('/doctor/consultation')"
              >接诊问诊</el-button
            >
          </div>
        </el-card>
      </el-col>
    </el-row>

    <h4 class="section-title">今日运营数据看板</h4>
    <!-- 数据看板 -->
    <el-row :gutter="20" class="data-panel" v-loading="loading">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card border-blue">
          <div class="data-title">今日挂号人次</div>
          <div class="data-value">{{ stats.todayRegistrations }}<span>人</span></div>
          <div class="data-trend">
            较昨日
            <span class="up"
              >5.2% <el-icon><Top /></el-icon
            ></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card border-orange">
          <div class="data-title">当前候诊人数</div>
          <div class="data-value">{{ stats.waitingPatients }}<span>人</span></div>
          <div class="data-trend">
            较昨日
            <span class="down"
              >2.1% <el-icon><Bottom /></el-icon
            ></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card border-green">
          <div class="data-title">今日门诊收入</div>
          <div class="data-value money">¥{{ stats.todayIncome.toFixed(2) }}</div>
          <div class="data-trend">
            较昨日
            <span class="up"
              >12.5% <el-icon><Top /></el-icon
            ></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card border-red">
          <div class="data-title">库存预警药品</div>
          <div class="data-value">{{ stats.stockWarnings }}<span>种</span></div>
          <div class="data-trend">
            较昨日
            <span class="down"
              >0% <el-icon><Bottom /></el-icon
            ></span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部任务与日志区 -->
    <el-row :gutter="20" class="bottom-panel">
      <!-- 待办任务 -->
      <el-col :span="14">
        <el-card shadow="never" class="task-card">
          <template #header>
            <div class="card-header">
              <span>我的待办任务</span>
              <el-button type="primary" link>查看全部</el-button>
            </div>
          </template>
          <div class="task-list">
            <div class="task-item" v-for="(task, index) in tasks" :key="index">
              <div class="task-name">{{ task.name }}</div>
              <div class="task-time">{{ task.time }}</div>
              <div class="task-status">
                <el-tag :type="task.type" effect="light" size="small">{{
                  task.status
                }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 系统日志 -->
      <el-col :span="10">
        <el-card shadow="never" class="log-card">
          <template #header>
            <div class="card-header">
              <span>系统日志与公告</span>
            </div>
          </template>
          <el-timeline class="notice-timeline">
            <el-timeline-item
              v-for="(log, index) in logs"
              :key="index"
              :type="log.type"
              :color="log.color"
              :timestamp="log.time"
            >
              <div class="log-content">
                <div class="log-title">{{ log.title }}</div>
                <div v-if="log.content" class="log-detail">{{ log.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div v-if="hasRole('ADMIN')" class="log-pagination">
            <el-pagination
              v-model:current-page="logPage.page"
              v-model:page-size="logPage.size"
              :page-sizes="[5, 10, 20]"
              small
              background
              layout="total, sizes, prev, pager, next"
              :total="logPage.total"
              @current-change="fetchDashboardData"
              @size-change="handleLogPageSizeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { useUserStore } from "../../store/user";
import request from "../../utils/request";

const userStore = useUserStore();
const loading = ref(false);
const stats = reactive({
  todayRegistrations: 0,
  waitingPatients: 0,
  todayIncome: 0,
  stockWarnings: 0,
});

const currentDate = computed(() => {
  const d = new Date();
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
});

const tasks = ref([]);
const logs = ref([]);
const logPage = reactive({ page: 1, size: 5, total: 0 });
const systemNotices = [
  {
    title: "系统公告",
    content: "请各岗位完成当日业务核对后再退出系统。",
    time: "今日",
    type: "success",
    color: "#67c23a",
  },
  {
    title: "运维提醒",
    content: "库存预警、挂号收费和处方发药数据已接入实时看板。",
    time: "实时",
    type: "warning",
    color: "#e6a23c",
  },
];

const hasRole = (role) => userStore.roles.includes("ADMIN") || userStore.roles.includes(role);
const hasAnyRole = (roles) =>
  userStore.roles.includes("ADMIN") || roles.some((role) => userStore.roles.includes(role));

const operationLabels = {
  LOGIN: "用户登录",
  REGISTRATION: "门诊挂号",
  PATIENT_UPDATE: "患者档案更新",
  PATIENT_CREATE: "患者建档",
  PATIENT_RECHARGE: "就诊卡充值",
  PATIENT_BALANCE_REFUND: "就诊卡退费",
  PRESCRIBE: "处方开立",
  PRESCRIPTION_PAY: "处方缴费",
  DISPENSE: "处方发药",
  DRUG_SAVE: "药品档案保存",
  DRUG_INBOUND: "药品入库",
  STOCKTAKE: "库存盘点",
  AUDIT_CLEAR: "日志清理",
};

const formatDateTime = (value) => {
  if (!value) return "刚刚";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ");
  const pad = (num) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const buildAuditLogItem = (log) => {
  const operation = operationLabels[log.operation] || log.operation || "系统操作";
  const username = log.username || "系统";
  const detail = log.detail || "暂无详细说明";
  return {
    title: `${operation} · ${username}`,
    content: detail,
    time: formatDateTime(log.time),
    type: "primary",
    color: "#409eff",
  };
};

const fetchDashboardData = async () => {
  loading.value = true;
  try {
    const [
      dashboard,
      warnings,
      auditLogs,
    ] = await Promise.all([
      hasRole("ADMIN") ? request.get("/api/analytics/dashboard") : Promise.resolve({}),
      hasRole("PHARMACY_ADMIN")
        ? request.get("/api/pharmacy/inventory/warnings")
        : Promise.resolve([]),
      hasRole("ADMIN")
        ? request.get("/api/audit/logs", { params: { page: logPage.page, size: logPage.size } })
        : Promise.resolve({ records: [], total: 0 }),
    ]);

    stats.todayRegistrations = Number(dashboard.todayRegistrations || 0);
    stats.waitingPatients = Number(dashboard.waitingPatients || 0);
    stats.todayIncome = Number(dashboard.todayIncome || 0);
    stats.stockWarnings = warnings.length || 0;

    const warningTasks = hasRole("PHARMACY_ADMIN")
      ? (warnings || []).slice(0, 3).map((drug) => ({
        name: `${drug.name} 库存低于预警线`,
        time: drug.updatedAt || drug.createdAt || "刚刚",
        status: "待处理",
        type: "warning",
      }))
      : [];
    const auditTask = hasRole("ADMIN")
      ? [{
        name: `审计日志累计 ${dashboard.auditLogCount || 0} 条`,
        time: "实时",
        status: "进行中",
        type: "info",
      }]
      : [];
    tasks.value = [
      ...warningTasks,
      ...auditTask,
      ...(warningTasks.length || auditTask.length
        ? []
        : [{
            name: "暂无待办事项",
            time: "实时",
            status: "正常",
            type: "success",
          }]),
    ];

    const latestAuditLogs = hasRole("ADMIN")
      ? (auditLogs.records || []).map(buildAuditLogItem)
      : [];
    logPage.total = auditLogs.total || 0;
    logs.value = [
      ...(logPage.page === 1 ? systemNotices : []),
      ...(latestAuditLogs.length
        ? latestAuditLogs
        : [{
            title: "系统日志",
            content: hasRole("ADMIN") ? "暂无最新审计日志。" : "当前账号暂无审计日志查看权限。",
            time: "实时",
            type: "info",
            color: "#909399",
          }]),
    ];
  } catch (error) {
    console.error(error);
    logs.value = [
      ...systemNotices,
      {
        title: "系统日志",
        content: "日志暂时无法加载，请稍后刷新看板。",
        time: "实时",
        type: "danger",
        color: "#f56c6c",
      },
    ];
  } finally {
    loading.value = false;
  }
};

const handleLogPageSizeChange = () => {
  logPage.page = 1;
  fetchDashboardData();
};

onMounted(() => {
  fetchDashboardData();
});
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.top-panel {
  margin-bottom: 25px;
  align-items: stretch;
}

.welcome-card,
.quick-action-card {
  min-height: 120px;
  height: 100%;
  border: none;
  border-radius: 4px;
}
.welcome-box {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 10px 0;
}
.user-avatar {
  background-color: #e4e7ed;
  color: #909399;
  margin-right: 20px;
}
.welcome-text .greeting {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}
.welcome-text .date-info {
  font-size: 14px;
  color: #909399;
}

.quick-action-card .action-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(118px, 1fr));
  gap: 14px;
  align-items: center;
  align-content: center;
  height: 100%;
  padding: 16px 0;
}
.action-buttons .el-button {
  width: 100%;
  min-width: 0;
  margin: 0;
  padding-left: 12px;
  padding-right: 12px;
}
.quick-action-card :deep(.el-card__body) {
  height: 100%;
}

.section-title {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #303133;
}

.data-panel {
  margin-bottom: 25px;
}
.data-card {
  border-radius: 4px;
  border: none;
  border-top: 4px solid transparent;
  padding: 5px;
}
.border-blue {
  border-top-color: #409eff;
}
.border-orange {
  border-top-color: #e6a23c;
}
.border-green {
  border-top-color: #67c23a;
}
.border-red {
  border-top-color: #f56c6c;
}

.data-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
}
.data-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
}
.data-value span {
  font-size: 16px;
  font-weight: normal;
  margin-left: 5px;
  color: #606266;
}
.data-value.money {
  font-size: 26px;
}

.data-trend {
  font-size: 13px;
  color: #909399;
}
.data-trend .up {
  color: #f56c6c;
  font-weight: bold;
}
.data-trend .down {
  color: #67c23a;
  font-weight: bold;
}

.bottom-panel {
  margin-top: 10px;
}
.task-card,
.log-card {
  border: none;
  border-radius: 4px;
  min-height: 350px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.task-list {
  padding: 0;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}
.task-item:last-child {
  border-bottom: none;
}
.task-name {
  flex: 1;
  font-size: 14px;
  color: #606266;
}
.task-time {
  width: 100px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
.task-status {
  width: 80px;
  text-align: right;
}
.notice-timeline {
  padding: 4px 0 0 2px;
}
.log-content {
  line-height: 1.5;
}
.log-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  word-break: break-word;
}
.log-detail {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
  word-break: break-word;
}
.log-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
