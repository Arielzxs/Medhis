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
    component: () => import("../layout/index.vue"), // 整体布局框架
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
        // meta 里面的 roles 控制哪些角色能看到这个页面
        meta: {
          title: "患者管理",
          roles: ["ADMIN", "REGISTRAR", "DOCTOR"],
          icon: "User",
        },
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
    // 如果没有 Token 且访问的不是登录页，强制跳回登录
    next("/login");
  } else {
    // 如果页面需要特定角色，且当前用户没有该角色，则跳回首页
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
