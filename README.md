# Medhis 医院信息管理系统

本项目是前后端分离的 HIS 医院信息管理系统：

- 前端：Vue 3 + Vite + Element Plus + Pinia + ECharts
- 后端：Spring Boot 3 + Maven 多模块 + MyBatis Plus + MySQL + JWT

## 目录结构

```text
Medhis
├── his-frontend          # 前端项目
└── neusoft-his-system    # 后端 Maven 多模块项目
```

后端模块：

- `his-common`：通用响应、异常、JWT、RBAC、审计能力
- `his-dal`：实体、Mapper、数据库初始化脚本
- `his-service`：认证、患者、医生、药房、财务、统计业务
- `his-api`：REST API 控制器和安全配置
- `his-boot-web`：Spring Boot 启动模块

## 数据库准备

后端默认连接本机 MySQL 的 `his_system` 数据库。启动前先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS his_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

如果你的 MySQL 用户名或密码不同，请修改：

```text
neusoft-his-system/his-boot-web/src/main/resources/application.yml
```

应用启动时会执行 `his-dal/src/main/resources/schema.sql` 初始化表结构。

## 启动后端

```bash
cd neusoft-his-system
mvn spring-boot:run -pl his-boot-web -am
```

后端接口默认路径以 `/api` 开头，例如：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/patients`
- `GET /api/analytics/dashboard`

## 启动前端

```bash
cd his-frontend
npm install
npm run dev
```

前端开发服务启动后，按终端显示的地址访问即可。Vite 已配置 `/api` 代理到后端：

```text
http://localhost:8080
```

## 构建验证

前端生产构建：

```bash
cd his-frontend
npm run build
```

后端源码编译：

```bash
cd neusoft-his-system
mvn -DskipTests compile
```

完整后端打包：

```bash
cd neusoft-his-system
mvn -DskipTests package
```

首次执行 Maven 或 npm 命令会下载依赖，耗时取决于网络和镜像源。
