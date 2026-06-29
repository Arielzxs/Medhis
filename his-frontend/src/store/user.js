import { defineStore } from "pinia";
import request from "../utils/request";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("his_token") || "",
    userId: localStorage.getItem("his_user_id") || "",
    username: localStorage.getItem("his_username") || "",
    roles: JSON.parse(localStorage.getItem("his_roles") || "[]"),
  }),
  actions: {
    // 登录动作：请求后端，保存信息到本地
    async login(loginForm) {
      const data = await request.post("/api/auth/login", loginForm);
      this.token = data.token;
      this.userId = data.userId;
      this.username = data.username;
      this.roles = data.roles;

      localStorage.setItem("his_token", data.token);
      localStorage.setItem("his_user_id", data.userId);
      localStorage.setItem("his_username", data.username);
      localStorage.setItem("his_roles", JSON.stringify(data.roles));
    },
    // 登出动作：清除本地信息
    logout() {
      this.token = "";
      this.userId = "";
      this.username = "";
      this.roles = [];
      localStorage.removeItem("his_token");
      localStorage.removeItem("his_user_id");
      localStorage.removeItem("his_username");
      localStorage.removeItem("his_roles");
    },
  },
});
