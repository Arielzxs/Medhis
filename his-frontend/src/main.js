import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import {
  ArrowDown,
  Bottom,
  Box,
  DataAnalysis,
  Expand,
  FirstAidKit,
  Fold,
  Money,
  Odometer,
  Platform,
  Plus,
  Refresh,
  Search,
  Setting,
  SuccessFilled,
  Ticket,
  Top,
  User,
  UserFilled,
} from "@element-plus/icons-vue";

const app = createApp(App);

const globalIcons = {
  ArrowDown,
  Bottom,
  Box,
  DataAnalysis,
  Expand,
  FirstAidKit,
  Fold,
  Money,
  Odometer,
  Platform,
  Plus,
  Refresh,
  Search,
  Setting,
  SuccessFilled,
  Ticket,
  Top,
  User,
  UserFilled,
};

Object.entries(globalIcons).forEach(([name, component]) => {
  app.component(name, component);
});

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

app.mount("#app");
