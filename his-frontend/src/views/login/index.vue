<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-content">
        <div class="brand-mark">
          <FirstAidKit />
        </div>
        <p class="eyebrow">MEDHIS · 智慧医院管理平台</p>
        <h1>让每一次诊疗协作<br />更清晰、更高效</h1>
        <p class="brand-description">
          连接挂号、医生、药房与财务，为医院日常运营提供安全可靠的一体化工作空间。
        </p>
        <div class="feature-list">
          <div class="feature-item">
            <CircleCheckFilled />
            <span>统一业务流程与数据看板</span>
          </div>
          <div class="feature-item">
            <CircleCheckFilled />
            <span>基于角色的精细权限控制</span>
          </div>
          <div class="feature-item">
            <CircleCheckFilled />
            <span>关键操作全程安全审计</span>
          </div>
        </div>
      </div>
      <div class="decor decor-one"></div>
      <div class="decor decor-two"></div>
    </section>

    <section class="form-panel">
      <div class="auth-card">
        <div class="mobile-brand">
          <span class="mobile-logo"><FirstAidKit /></span>
          <span>MEDHIS</span>
        </div>
        <div class="card-heading">
          <span class="welcome-tag">欢迎回来</span>
          <h2>登录您的工作台</h2>
          <p>请输入账号信息以继续使用系统</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
              placeholder="请输入用户名或职工工号"
              :prefix-icon="User"
              autocomplete="username"
              clearable
          />
        </el-form-item>
          <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="submit-btn"
            :loading="loading"
          >
            登录系统
            <ArrowRight class="button-icon" />
          </el-button>
        <div class="toggle-link">
          <span>没有账号？</span>
            <button type="button" @click="goToRegister">创建账号</button>
        </div>
      </el-form>
        <p class="security-tip"><Lock /> 系统采用加密传输保护您的账号信息</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../../store/user";
import { ElMessage } from "element-plus";
import {
  ArrowRight,
  CircleCheckFilled,
  FirstAidKit,
  Lock,
  User,
} from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const loginFormRef = ref(null);

const loginForm = reactive({ username: "", password: "" });

const loginRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: ["blur", "change"] },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: ["blur", "change"] },
  ],
};

const handleLogin = async () => {
  if (loading.value) return;
  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await userStore.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    });
    ElMessage.success("登录成功");
    router.push("/");
  } finally {
    loading.value = false;
  }
};

const goToRegister = () => {
  router.push("/register");
};
</script>

<style scoped>
@import "../auth.css";
</style>
