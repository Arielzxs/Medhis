import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "../store/user";

const routes = [
  {
    path: "/login",
    component: () => import("../views/login/index.vue"),
    hidden: true,
  },
  {
    path: "/register",
    component: () => import("../views/register/index.vue"),
    hidden: true,
  },
  {
    path: "/",
    component: () => import("../layout/index.vue"),
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        component: () => import("../views/dashboard/index.vue"),
        meta: { title: "数据看板", icon: "Odometer" },
      },
      {
        path: "patient",
        component: () => import("../views/doctor/ParentView.vue"), // 复用空壳容器
        redirect: "/patient/registration",
        meta: {
          title: "患者管理",
          roles: ["ADMIN", "REGISTRAR", "DOCTOR"],
          icon: "User",
        },
        children: [
          {
            path: "registration",
            component: () => import("../views/patient/registration.vue"),
            meta: { title: "门诊挂号", roles: ["ADMIN", "REGISTRAR"] },
          },
          {
            path: "tracking",
            component: () => import("../views/patient/tracking.vue"),
            meta: {
              title: "就诊状态追踪",
              roles: ["ADMIN", "REGISTRAR", "DOCTOR"],
            },
          },
        ],
      },
      {
        path: "doctor",
        // 1. 关键点：指定父级组件为刚刚创建的含有 <router-view /> 的中转视图
        component: () => import("../views/doctor/ParentView.vue"),
        // 2. 关键点：当点击“医生工作站”进入 /doctor 时，自动跳转到第一个子页面，防止白屏
        redirect: "/doctor/schedule",
        meta: {
          title: "医生工作站",
          roles: ["ADMIN", "DOCTOR"],
          icon: "FirstAidKit",
        },
        children: [
          {
            path: "schedule",
            component: () => import("../views/doctor/schedule.vue"),
            meta: { title: "人员排班", roles: ["ADMIN", "DOCTOR"] },
          },
          {
            path: "consultation",
            component: () => import("../views/doctor/index.vue"),
            meta: { title: "接诊问诊", roles: ["ADMIN", "DOCTOR"] },
          },
        ],
      },
      {
        path: "pharmacy",
        component: () => import("../views/pharmacy/index.vue"),
        meta: {
          title: "药房管理",
          roles: ["ADMIN", "PHARMACY_ADMIN"],
          icon: "Box",
        },
      },
      {
        path: "finance",
        component: () => import("../views/finance/index.vue"),
        meta: { title: "财务管理", roles: ["ADMIN", "FINANCE"], icon: "Money" },
      },
      {
        path: "analytics",
        component: () => import("../views/analytics/ParentView.vue"),
        redirect: "/analytics/workload",
        meta: {
          title: "统计分析",
          roles: ["ADMIN", "FINANCE", "DOCTOR"],
          icon: "DataAnalysis",
        },
        children: [
          {
            path: "workload",
            component: () => import("../views/analytics/workload.vue"),
            meta: { title: "工作量统计", roles: ["ADMIN", "DOCTOR"] },
          },
          {
            path: "revenue",
            component: () => import("../views/analytics/revenue.vue"),
            meta: { title: "财务收支报表", roles: ["ADMIN", "FINANCE"] },
          },
          {
            path: "drug",
            component: () => import("../views/analytics/drug.vue"),
            meta: { title: "药品消耗排行", roles: ["ADMIN", "PHARMACY_ADMIN"] },
          },
        ],
      },
      {
        path: "system",
        component: () => import("../views/system/index.vue"),
        meta: {
          title: "权限与基础支撑",
          roles: ["ADMIN"], // 仅限超级管理员访问
          icon: "Setting",
        },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 全局路由守卫（增加了对 /register 的放行检查）
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();

  // 1. 如果去登录或者注册页面，直接放行
  if (to.path === "/login" || to.path === "/register") {
    next();
  }
  // 2. 如果没有 Token，强制去登录页
  else if (!userStore.token) {
    next("/login");
  }
  // 3. 有 Token，进行RBAC角色权限判定
  else {
    if (
      to.meta.roles &&
      !to.meta.roles.some((r) => userStore.roles.includes(r))
    ) {
      next("/dashboard");
    } else {
      next();
    }
  }
});

export default router;
