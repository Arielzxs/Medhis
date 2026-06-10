<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">Neusoft HIS</div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="route in menus" :key="route.path">
          <el-menu-item
            :index="'/' + route.path"
            v-if="hasAuth(route.meta.roles)"
          >
            <el-icon><component :is="route.meta.icon" /></el-icon>
            <span>{{ route.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span>欢迎，{{ userStore.username }}</span>
          <el-button
            type="danger"
            link
            @click="handleLogout"
            style="margin-left: 15px"
            >退出登录</el-button
          >
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../store/user";

const router = useRouter();
const userStore = useUserStore();

// 提取 router 中的子路由作为左侧菜单
const menus = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === "/");
  return layoutRoute ? layoutRoute.children : [];
});

// 权限判断函数
const hasAuth = (roles) => {
  if (!roles) return true;
  return roles.some((role) => userStore.roles.includes(role));
};

const handleLogout = () => {
  userStore.logout();
  router.push("/login");
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
</style>
