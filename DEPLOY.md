# 智能药箱管理系统 — 本地部署指南

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 8 或以上 | 推荐 AdoptOpenJDK 8 / Oracle JDK 8 |
| Maven | 3.6+ | 项目自带 Maven Wrapper，无需手动安装 |
| Node.js | 16+ | 推荐 18 LTS |
| PostgreSQL | 12+ | 数据库服务 |
| Redis | 6+ | 验证码缓存 |

> **Windows 用户**：Maven Wrapper 使用 `mvnw.cmd`，Linux/macOS 使用 `mvnw`。

---

## 第一步：克隆项目

```bash
git clone https://github.com/wangzihan-hunan/smart-medicine-box.git
cd smart-medicine-box
```

---

## 第二步：配置数据库

### 2.1 创建 PostgreSQL 数据库

使用你本地的 PostgreSQL 客户端（psql / pgAdmin / Navicat）执行：

```sql
CREATE DATABASE medicine_box
  WITH ENCODING = 'UTF8'
  TEMPLATE = template0;
```

> 使用 `template0` 是为了避免字符集排序规则冲突。

### 2.2 修改数据库连接配置（可选）

数据库默认连接信息：

| 配置项 | 默认值 |
|--------|--------|
| 地址 | `localhost:9456` |
| 用户名 | `postgres` |
| 密码 | `wzh@10086` |
| 数据库 | `medicine_box` |

如果你的本地 PostgreSQL 端口或密码不同，可通过环境变量覆盖：

```bash
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/medicine_box"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="你的密码"

# Linux / macOS
export DB_URL="jdbc:postgresql://localhost:5432/medicine_box"
export DB_USERNAME="postgres"
export DB_PASSWORD="你的密码"
```

---

## 第三步：配置 Redis

确保 Redis 服务已启动，默认连接 `localhost:6379`（无密码）。

可通过环境变量覆盖：

```bash
# Windows PowerShell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="你的密码"   # 如果没有密码则留空

# Linux / macOS
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
```

### 安装 Redis（如果尚未安装）

- **Windows**：下载 [Redis for Windows](https://github.com/tporadowski/redis/releases) 或使用 `winget install Redis`
- **macOS**：`brew install redis && brew services start redis`
- **Linux**：`sudo apt install redis-server`（Ubuntu）或 `sudo yum install redis`（CentOS）

---

## 第四步：启动后端

```bash
cd springboot-backend
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

首次启动会自动：
- 下载 Maven 依赖
- 执行 Flyway 数据库迁移脚本，创建所有表结构
- 初始化默认账号（见下方）

启动成功后访问：http://localhost:8080/actuator/health

---

## 第五步：启动前端

打开**新的终端窗口**：

```bash
cd qianduan
npm install
npm run dev
```

启动后访问：http://localhost:3002

> 前端开发服务器已配置代理，`/api` 请求会自动转发到 `http://localhost:8080`。

---

## 默认账号

| 角色 | 手机号 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `13800138000` | `123456` | 首次登录强制修改密码 |
| 普通用户 | `13900139000` | `123456` | 测试用 |

---

## 运行测试

```bash
cd springboot-backend
.\mvnw.cmd test    # Windows
./mvnw test        # Linux / macOS
```

测试使用 H2 内存数据库，无需额外配置。

---

## 项目结构

```
smart-medicine-box/
├── springboot-backend/          # 后端 (Spring Boot 2.7 + Java 8)
│   ├── src/main/java/           # 业务代码
│   ├── src/main/resources/      # 配置文件、数据库迁移脚本
│   ├── src/test/                # 单元测试与集成测试
│   └── pom.xml                  # Maven 依赖
├── qianduan/                    # 前端 (Vue 3 + Vite 5 + Element Plus)
│   ├── src/views/               # 页面组件
│   ├── src/components/          # 公共组件
│   ├── src/stores/              # Pinia 状态管理
│   ├── src/utils/               # 工具函数、API 封装
│   └── package.json
└── DEPLOY.md                    # 你正在阅读的文档
```

---

## 可选配置

### AI 视觉识别（阿里百炼通义千问）

此功能默认关闭。如需启用，设置环境变量：

```bash
$env:AI_QWEN_ENABLED="true"
$env:AI_QWEN_API_KEY="你的 API Key"
```

### Flyway 数据库迁移

默认使用 `ddl-auto: update`（Hibernate 自动建表）。如需启用 Flyway 版本化管理，将 `application.yml` 中 `flyway.enabled` 改为 `true`。

---

## 常见问题

**Q: 启动后端报 "Port 8080 was already in use"**

```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# Linux / macOS
lsof -i :8080
kill -9 <PID>
```

**Q: 数据库连接失败**

确认 PostgreSQL 服务已启动，且端口、用户名、密码与 `application.yml` 配置一致。

**Q: 前端 `npm install` 很慢**

使用国内镜像：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

**Q: 前端页面空白 / 接口 CORS 报错**

确认前端运行端口在 CORS 白名单中。`application.yml` 中 `cors.allowed-origins` 默认包含 `localhost:3000` 到 `3004`。如使用其他端口，需手动添加。