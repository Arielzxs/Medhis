<!-- src/views/dashboard/index.vue -->
<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎区与快捷操作 -->
    <el-row :gutter="20" class="top-panel">
      <el-col :span="16">
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
      <el-col :span="8">
        <el-card shadow="never" class="quick-action-card">
          <div class="action-buttons">
            <el-button
              type="primary"
              size="large"
              @click="$router.push('/patient/registration')"
              >快捷挂号</el-button
            >
            <el-button
              type="success"
              size="large"
              @click="$router.push('/finance')"
              >门诊收费</el-button
            >
            <el-button
              type="warning"
              size="large"
              @click="$router.push('/pharmacy')"
              >发药工作台</el-button
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
          <div class="data-value">342<span>人</span></div>
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
          <div class="data-value">45<span>人</span></div>
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
          <div class="data-value money">¥125,860.50</div>
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
          <div class="data-value">12<span>种</span></div>
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
          <el-timeline>
            <el-timeline-item
              v-for="(log, index) in logs"
              :key="index"
              :type="log.type"
              :color="log.color"
              :timestamp="log.time"
            >
              {{ log.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useUserStore } from "../../store/user";
import request from "../../utils/request";

const userStore = useUserStore();
const loading = ref(false);

const currentDate = computed(() => {
  const d = new Date();
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
});

// 模拟待办任务数据
const tasks = ref([
  {
    name: "内科门诊处方退费审核 (张三)",
    time: "10:30",
    status: "待处理",
    type: "danger",
  },
  {
    name: "药房日常库存盘点",
    time: "14:00",
    status: "进行中",
    type: "warning",
  },
  {
    name: "心血管内科排班计划确认",
    time: "16:00",
    status: "未开始",
    type: "info",
  },
  {
    name: "阿莫西林胶囊采购入库审核",
    time: "昨天",
    status: "已完成",
    type: "success",
  },
]);

// 模拟系统日志数据
const logs = ref([
  {
    content: "系统成功执行每日数据备份",
    time: "2026-06-11 02:00",
    type: "success",
    color: "#67c23a",
  },
  {
    content: "系统管理员 张翔硕 登录系统",
    time: "2026-06-11 08:30",
    type: "primary",
    color: "#409eff",
  },
  {
    content: "药房模块触发低库存预警提醒",
    time: "2026-06-11 09:15",
    type: "warning",
    color: "#e6a23c",
  },
]);

const fetchDashboardData = async () => {
  loading.value = true;
  try {
    // 实际项目中可以向后端请求真实的仪表盘数据
    await request.get("/api/analytics/dashboard");
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
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
}

.welcome-card,
.quick-action-card {
  height: 120px;
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
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 100%;
  padding: 18px 0;
}
.action-buttons .el-button {
  flex: 1;
  margin: 0 10px;
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
</style>
