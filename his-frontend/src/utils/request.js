import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "../store/user";
import router from "../router";

const request = axios.create({
  baseURL: "",
  timeout: 10000,
});

let lastAuthErrorAt = 0;

const showErrorOnce = (message) => {
  const isAuthError =
    message?.includes("无权限") ||
    message?.includes("权限不足") ||
    message?.includes("登录已过期");
  if (isAuthError) {
    const now = Date.now();
    if (now - lastAuthErrorAt < 1500) return;
    lastAuthErrorAt = now;
  }
  ElMessage.error(message || "操作失败");
};

// 请求拦截器：发送请求前自动加上 Token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers["Authorization"] = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 响应拦截器：处理后端的 ApiResponse 统一格式
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    // 如果没有 success 字段，说明可能不是统一的 API 响应，直接返回
    if (res.success === undefined) return res;

    // 如果 success 为 true，直接返回 data 里面的核心数据
    if (res.success) return res.data;

    // 如果 success 为 false，弹出后端的错误信息
    showErrorOnce(res.message || "操作失败");
    return Promise.reject(new Error(res.message || "Error"));
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      showErrorOnce("权限不足或登录已过期，请重新登录");
      const userStore = useUserStore();
      userStore.logout();
      router.push("/login");
    } else {
      showErrorOnce(error.message || "网络异常");
    }
    return Promise.reject(error);
  },
);

export default request;
