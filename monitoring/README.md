# 智能药箱 - 监控告警系统

本目录包含完整的监控告警栈，基于 Prometheus + Grafana + Loki + AlertManager 实现。

## 组件架构

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ Spring Boot │───>│  Prometheus  │<───│ AlertManager│
│ /actuator/  │    │  (时序数据库) │    │  (告警管理) │
│  prometheus │    └──────┬───────┘    └──────┬──────┘
└─────────────┘           │                    │
                          ▼                    ▼
                    ┌──────────┐         ┌─────────┐
                    │ Grafana  │         │  Email  │
                    │ (可视化) │         │ (邮件)  │
                    └────┬─────┘         └─────────┘
                         │
                         ▼
                    ┌──────────┐         ┌──────────┐
                    │   Loki   │<────────│ Promtail │
                    │ (日志DB) │         │(日志采集)│
                    └──────────┘         └──────────┘
```

## 服务列表

| 组件 | 端口 | 默认账号 | 用途 |
|------|------|---------|------|
| Prometheus | 9090 | - | 指标采集与存储 |
| Grafana | 3001 | admin/admin | 监控可视化 |
| AlertManager | 9093 | - | 告警管理 |
| Loki | 3100 | - | 日志聚合 |
| Promtail | - | - | 日志采集器 |
| Node Exporter | 9100 | - | 主机监控 |
| PostgreSQL Exporter | 9187 | - | 数据库监控 |
| Redis Exporter | 9121 | - | 缓存监控 |

## 启动步骤

### 1. 启动应用服务

```bash
cd springboot-backend
docker-compose up -d
```

### 2. 启动监控服务

```bash
cd monitoring
docker-compose -f docker-compose.monitoring.yml up -d
```

### 3. 访问服务

- **Grafana**: http://localhost:3001
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **应用指标**: http://localhost:8080/actuator/prometheus

## 告警规则

### 1. API 异常率告警
- **规则**: 过去 5 分钟内 5xx 错误率 > 5%
- **级别**: critical
- **触发条件**: 持续 2 分钟

### 2. API 响应时间告警
- **规则**: P95 响应时间 > 2 秒
- **级别**: warning
- **触发条件**: 持续 3 分钟

### 3. 严重响应时间告警
- **规则**: P95 响应时间 > 5 秒
- **级别**: critical
- **触发条件**: 持续 2 分钟

### 4. JVM 堆内存告警
- **规则**: JVM 堆内存使用率 > 85%
- **级别**: warning
- **触发条件**: 持续 3 分钟

### 5. CPU 使用率告警
- **规则**: 进程 CPU 使用率 > 85%
- **级别**: warning
- **触发条件**: 持续 5 分钟

### 6. 业务指标告警
- **登录失败率过高**: 登录失败率 > 50% （可能存在暴力破解）
- **AI 识别失败突增**: AI 识别失败率突增
- **数据库连接池告警**: 数据库连接池使用率 > 80%

## 业务指标

通过 `MetricsService` 采集的指标：

| 指标名 | 类型 | 标签 | 含义 |
|--------|------|------|------|
| business_user_register_total | Counter | - | 用户注册总数 |
| business_user_login_total | Counter | result | 用户登录次数 |
| business_ai_recognition_total | Counter | result | AI 识别调用次数 |
| business_ocr_recognition_total | Counter | - | OCR 识别调用次数 |
| business_prescription_upload_total | Counter | - | 药方上传次数 |
| business_exception_total | Counter | type | 业务异常次数 |
| business_api_latency | Timer | api | API 调用耗时 |
| business_user_active | Gauge | - | 活跃用户数 |
| business_user_online | Gauge | - | 在线用户数 |

## API 管理接口

通过管理员后台可以查看和管理告警：

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/admin/alerts | GET | 告警列表（分页） |
| /api/v1/admin/alerts/stats | GET | 告警统计 |
| /api/v1/admin/alerts/{id}/handle | PUT | 处理告警 |
| /api/v1/admin/metrics/summary | GET | 业务指标摘要 |

## 配置告警通知

在 `springboot-backend/src/main/resources/application.yml` 中配置：

```yaml
alerting:
  enabled: true
  email:
    enabled: true
    from: alert@medicine-box.com
    to: admin@medicine-box.com
    host: smtp.example.com
    port: 465
    username: alert@medicine-box.com
    password: your-password
```

## 启用应用内告警

应用内置告警服务（不依赖 Prometheus AlertManager），通过 `AlertingService` 实现：

- 异常率监控
- 响应时间监控
- CPU 负载监控
- JVM 内存监控

通过 `alerting.enabled=true` 启用，告警记录存储在 `alert_records` 表中。

## 目录结构

```
monitoring/
├── docker-compose.monitoring.yml    # 监控服务编排
├── prometheus/
│   ├── prometheus.yml                # Prometheus 配置
│   └── alert_rules.yml               # 告警规则
├── alertmanager/
│   └── alertmanager.yml              # 告警管理器配置
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/datasources.yml
│   │   └── dashboards/default.yml
│   └── dashboards/
│       └── medicine-box-dashboard.json
├── loki/
│   └── loki-config.yml               # Loki 配置
└── promtail/
    └── promtail-config.yml           # Promtail 配置
```
