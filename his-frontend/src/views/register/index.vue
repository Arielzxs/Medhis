<template>
  <main class="auth-page">
    <section class="brand-panel">
      <div class="brand-content">
        <div class="brand-mark"><FirstAidKit /></div>
        <p class="eyebrow">MEDHIS · 智慧医院管理平台</p>
        <h1>加入统一协作平台<br />专注每一位患者</h1>
        <p class="brand-description">
          根据您的岗位创建专属工作空间，系统将自动配置对应的业务菜单和操作权限。
        </p>
        <div class="role-preview">
          <span><FirstAidKit /> 医生工作站</span>
          <span><Tickets /> 挂号服务</span>
          <span><Box /> 药房管理</span>
          <span><TrendCharts /> 财务分析</span>
        </div>
      </div>
      <div class="decor decor-one"></div>
      <div class="decor decor-two"></div>
    </section>

    <section class="form-panel register-panel">
      <div class="auth-card register-card">
        <div class="mobile-brand">
          <span class="mobile-logo"><FirstAidKit /></span>
          <span>MEDHIS</span>
        </div>
        <div class="card-heading">
          <span class="welcome-tag">创建账号</span>
          <h2>填写您的职工信息</h2>
          <p>请选择真实岗位，注册后将获得对应系统权限</p>
        </div>
      <el-form
          ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
          label-position="top"
          size="large"
          @submit.prevent="handleRegister"
      >
          <div class="form-row">
            <el-form-item label="用户名 / 工号" prop="username">
          <el-input
            v-model="registerForm.username"
                placeholder="设置登录用户名"
                :prefix-icon="User"
                autocomplete="username"
                clearable
          />
        </el-form-item>
            <el-form-item label="真实姓名" prop="name">
          <el-input
            v-model="registerForm.name"
                placeholder="请输入真实姓名"
                :prefix-icon="Postcard"
                autocomplete="name"
                clearable
          />
        </el-form-item>
          </div>

          <el-form-item label="岗位身份" prop="role">
            <el-select
              v-model="registerForm.role"
              placeholder="请选择您的岗位身份"
              class="role-select"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              >
                <div class="role-option">
                  <span>{{ role.label }}</span>
                  <small>{{ role.description }}</small>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <div class="form-row">
            <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
                placeholder="至少 6 位字符"
                :prefix-icon="Lock"
                autocomplete="new-password"
                show-password
          />
        </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
                placeholder="请再次输入密码"
                :prefix-icon="Lock"
                autocomplete="new-password"
                show-password
                @keyup.enter="handleRegister"
          />
        </el-form-item>
          </div>

          <el-button
            type="primary"
            native-type="submit"
            class="submit-btn"
            :loading="loading"
          >
            创建账号
            <ArrowRight class="button-icon" />
          </el-button>
        <div class="toggle-link">
          <span>已有账号？</span>
            <button type="button" @click="goToLogin">返回登录</button>
        </div>
      </el-form>
        <p class="security-tip"><Lock /> 管理员身份仅可由系统后台分配</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import request from "../../utils/request";
import { ElMessage } from "element-plus";
import {
  ArrowRight,
  Box,
  FirstAidKit,
  Lock,
  Postcard,
  Tickets,
  TrendCharts,
  User,
} from "@element-plus/icons-vue";

const router = useRouter();
const loading = ref(false);
const registerFormRef = ref(null);

const registerForm = reactive({
  username: "",
  name: "",
  password: "",
  confirmPassword: "",
  role: "",
});

const roleOptions = [
  { label: "门诊医生", value: "DOCTOR", description: "接诊、病历与处方" },
  { label: "挂号员", value: "REGISTRAR", description: "挂号与就诊追踪" },
  {
    label: "药房管理员",
    value: "PHARMACY_ADMIN",
    description: "药品库存与发药",
  },
  { label: "财务人员", value: "FINANCE", description: "收费与财务报表" },
];

const validateConfirmPassword = (rule, value, callback) => {
  if (value === "") {
    callback(new Error("请再次输入密码"));
  } else if (value !== registerForm.password) {
    callback(new Error("两次输入的密码不一致!"));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: ["blur", "change"] },
    {
      pattern: /^[A-Za-z0-9_-]{3,20}$/,
      message: "请输入 3-20 位字母、数字、下划线或短横线",
      trigger: "blur",
    },
  ],
  name: [
    { required: true, message: "请输入真实姓名", trigger: ["blur", "change"] },
    { min: 2, max: 20, message: "姓名长度应为 2-20 个字符", trigger: "blur" },
  ],
  role: [
    { required: true, message: "请选择岗位身份", trigger: ["blur", "change"] },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: ["blur", "change"] },
    { min: 6, max: 32, message: "密码长度应为 6-32 位", trigger: "blur" },
  ],
  confirmPassword: [
    {
      required: true,
      validator: validateConfirmPassword,
      trigger: ["blur", "change"],
    },
  ],
};

const handleRegister = async () => {
  if (loading.value) return;
  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await request.post("/api/auth/register", {
      username: registerForm.username.trim(),
      name: registerForm.name.trim(),
      password: registerForm.password,
      role: registerForm.role,
    });
    ElMessage.success("注册成功，已为您配置岗位权限");
    router.push("/login");
  } finally {
    loading.value = false;
  }
};

const goToLogin = () => {
  router.push("/login");
};
</script>

<style scoped>
@import "../auth.css";
</style>
