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
        component: () => import("../views/patient/index.vue"),
        meta: {
          title: "患者管理",
          roles: ["ADMIN", "REGISTRAR", "DOCTOR"],
          icon: "User",
        },
      },
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
