<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 class="title">HIS 医疗系统登录</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
            >登 录</el-button
          >
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../../store/user";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref(null);
const loading = ref(false);
const form = reactive({ username: "admin", password: "123456" }); // 可以把你的测试账号写在这

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await userStore.login(form);
        ElMessage.success("登录成功");
        router.push("/");
      } catch (error) {
        // 请求失败已经在 request.js 拦截器里处理了，这里静默捕捉
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}
.login-card {
  width: 400px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.title {
  text-align: center;
  margin: 0;
  color: #303133;
}
.login-btn {
  width: 100%;
  font-size: 16px;
}
</style>
