<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon v-if="isCollapse" :size="24" color="#fff"
          ><Platform
        /></el-icon>
        <span v-else>云医院 HIS</span>
      </div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :collapse="isCollapse"
        :collapse-transition="false"
        class="el-menu-vertical"
      >
        <template v-for="route in menus" :key="route.path">
          <el-sub-menu
            v-if="route.children && route.children.length > 0"
            :index="'/' + route.path"
          >
            <template #title>
              <el-icon v-if="route.meta?.icon"
                ><component :is="route.meta.icon"
              /></el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>

            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="'/' + route.path + '/' + child.path"
            >
              <template #title>{{ child.meta?.title }}</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="'/' + route.path">
            <el-icon v-if="route.meta?.icon"
              ><component :is="route.meta.icon"
            /></el-icon>
            <template #title>{{ route.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item
              v-if="$route.matched.length > 1 && $route.matched[1].meta.title"
              >{{ $route.matched[1].meta.title }}</el-breadcrumb-item
            >
            <el-breadcrumb-item
              v-if="$route.matched.length > 2 && $route.matched[2].meta.title"
              >{{ $route.matched[2].meta.title }}</el-breadcrumb-item
            >
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-dropdown">
              <el-avatar
                :size="32"
                style="background-color: #409eff; margin-right: 8px"
              >
                {{ userStore.username.charAt(0).toUpperCase() }}
              </el-avatar>
              {{ userStore.username }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item
                  divided
                  command="logout"
                  style="color: #f56c6c"
                  >退出登录</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../store/user";

const router = useRouter();
const userStore = useUserStore();
const isCollapse = ref(false);

// 获取 layout 下的所有子路由作为菜单
const menus = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === "/");
  if (!layoutRoute) return [];
  return layoutRoute.children
    .filter((route) => !route.hidden && hasAuth(route.meta))
    .map((route) => {
      if (!route.children?.length) return route;
      const children = route.children.filter((child) => hasAuth(child.meta));
      return { ...route, children };
    })
    .filter((route) => !route.children || route.children.length > 0);
});

// 检查当前用户是否有权限访问该路由
const hasAuth = (meta) => {
  const roles = meta?.roles;
  if (meta?.excludeRoles?.some((role) => userStore.roles.includes(role))) return false;
  if (!roles) return true;
  if (userStore.roles.includes("ADMIN")) return true;
  return roles.some((role) => userStore.roles.includes(role));
};

// 下拉菜单操作
const handleCommand = (command) => {
  if (command === "logout") {
    userStore.logout();
    router.push("/login");
  }
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3643;
  white-space: nowrap;
}
.el-menu-vertical {
  border-right: none;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.collapse-btn:hover {
  color: #409eff;
}
.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}
.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}

/* 页面切换动画 */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
