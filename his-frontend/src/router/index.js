import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "../store/user";

const routes = [
  {
    path: "/login",
    component: () => import("../views/login/index.vue"),
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
        component: () => import("../views/patient/index.vue"),
        meta: {
          title: "患者管理",
          roles: ["ADMIN", "REGISTRAR", "DOCTOR"],
          icon: "User",
        },
      },
      // ---- 下面是新增的三个核心业务模块 ----
      {
        path: "doctor",
        component: () => import("../views/doctor/index.vue"),
        meta: {
          title: "医生工作站",
          roles: ["ADMIN", "DOCTOR"],
          icon: "FirstAidKit",
        },
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
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 全局路由守卫（门禁系统）
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  if (to.path === "/login") {
    next();
  } else if (!userStore.token) {
    next("/login");
  } else {
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
