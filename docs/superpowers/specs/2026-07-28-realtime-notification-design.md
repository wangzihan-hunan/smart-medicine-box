# 通知实时推送功能设计文档

## 1. 目标与背景

### 1.1 目标
为智能药箱系统增加通知实时推送能力，让用户无需手动刷新页面即可收到：
- 用药提醒到期通知
- 药品库存/过期预警
- 管理员系统公告
- 账号状态变更通知（封禁/冻结/解封）

### 1.2 设计约束
- 后端基于 Spring Boot 2.7.x + Java 8
- 前端基于 Vue 3 + Vite + Element Plus
- 复用现有 `notifications` 表，保证离线消息不丢失
- 所有 API 路径带 `/api/v1` 版本前缀
- 必须符合现有 JWT 认证体系

## 2. 需求确认

| 需求项 | 确认结果 |
|--------|----------|
| 推送场景 | 用药提醒、库存/过期预警、管理员公告、账号状态变更 |
| 实时性要求 | 秒级实时，延迟不超过 3 秒 |
| 离线消息处理 | 数据库持久化 + 在线推送 |
| 多端在线 | 支持同一账号多端同时在线 |
| 技术方案 | WebSocket + STOMP |
| 用药提醒 | 支持准时 + 提前 5/10/15/30 分钟提醒 |
| 广播范围 | 公告广播，个人通知定向 |
| 前端提示 | 右上角 Toast 提示 |
| 桌面通知 | 暂不需要，保留扩展接口 |
| 定时任务 | Spring `@Scheduled` 固定频率扫描 |
| 多端状态同步 | 已读/清空操作需同步到所有在线端 |

## 3. 整体架构

```
触发源（后端）
  ├── 定时任务：用药提醒扫描
  ├── 药品 CRUD：库存/过期检查
  ├── 管理员操作：发布公告、修改用户状态
  └── 其他业务：套餐到期、系统告警等
           │
           ▼
   NotificationService
    1. 写入 notifications 表（离线可见）
    2. 调用 WebSocketPushService 推送
           │
           ▼
   SimpMessagingTemplate
   convertAndSendToUser(userId, "/queue/notifications", msg)
           │
           ▼
   Spring 内置 STOMP Broker
   /user/queue/notifications
           │
           ▼ (WebSocket 连接 + JWT 握手认证)
   前端 stompjs 客户端
    - 订阅个人通知频道
    - 收到消息 → 更新未读数 + Toast 提示
    - 断线自动重连
```

## 4. 数据模型与消息格式

### 4.1 现有实体复用
复用 `Notification` 实体和 `notifications` 表，字段已包含：
- `id`, `user_id`, `title`, `content`, `type`, `read_status`, `created_at`, `read_at`

### 4.2 WebSocket 推送消息 DTO
新增 `NotificationPushMessage`：

```java
public class NotificationPushMessage {
    private Integer id;           // 通知ID
    private String title;         // 标题
    private String content;       // 内容
    private String type;          // 类型
    private Boolean readStatus;   // 是否已读
    private String createdAt;     // 创建时间（ISO字符串）
    private String action;        // 动作类型：NEW / READ / READ_ALL / DELETE / DELETE_ALL
    private Integer unreadCount;  // 当前未读数（可选）
}
```

### 4.3 消息动作类型说明
- `NEW`：新增通知
- `READ`：单条已读
- `READ_ALL`：全部已读
- `DELETE`：删除单条
- `DELETE_ALL`：清空全部

## 5. 后端设计

### 5.1 新增依赖（pom.xml）

```xml
<!-- Spring Boot WebSocket + STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 5.2 WebSocket 配置

新增 `WebSocketConfig.java`：

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，前缀 /topic 用于广播，/queue 用于点对点
        config.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息到服务端的前缀
        config.setApplicationDestinationPrefixes("/app");
        // 点对点发送时前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*") // 开发环境，生产环境按 CorsConfig 配置
                .withSockJS();
    }
}
```

### 5.3 JWT 握手认证拦截器

新增 `WebSocketAuthInterceptor.java`：

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String phone = jwtUtil.extractPhone(token);
                if (phone != null && jwtUtil.validateToken(token)) {
                    accessor.setUser(new StompPrincipal(phone));
                }
            }
        }
        return message;
    }
}
```

### 5.4 用户 Principal 实现

新增 `StompPrincipal.java`：

```java
public class StompPrincipal implements Principal {
    private final String name;
    public StompPrincipal(String name) { this.name = name; }
    @Override
    public String getName() { return name; }
}
```

### 5.5 WebSocket 安全配置

新增 `WebSocketSecurityConfig.java`：

```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpDestMatchers("/app/**").authenticated()
            .simpSubscribeDestMatchers("/user/queue/notifications").authenticated()
            .simpSubscribeDestMatchers("/topic/announcements").hasRole("ADMIN")
            .anyMessage().denyAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
```

### 5.6 WebSocket 推送服务

新增 `WebSocketPushService.java`：

```java
@Service
public class WebSocketPushService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 向指定用户推送通知
     */
    public void pushToUser(Integer userId, NotificationPushMessage message) {
        messagingTemplate.convertAndSendToUser(
            String.valueOf(userId),
            "/queue/notifications",
            message
        );
    }

    /**
     * 向所有在线用户广播公告
     */
    public void broadcastAnnouncement(NotificationPushMessage message) {
        messagingTemplate.convertAndSend("/topic/announcements", message);
    }

    /**
     * 推送状态同步指令
     */
    public void syncNotificationState(Integer userId, String action) {
        NotificationPushMessage message = new NotificationPushMessage();
        message.setAction(action);
        pushToUser(userId, message);
    }
}
```

### 5.7 用药提醒定时任务

新增 `ReminderScheduler.java`：

```java
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;

    /**
     * 每 30 秒扫描一次到期提醒
     */
    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void checkDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        // 查询已经到期且未发送过通知的提醒
        List<Reminder> dueReminders = reminderRepository.findByIsActiveTrueAndNextRemindTimeLessThanEqual(now);

        for (Reminder reminder : dueReminders) {
            notificationService.sendNotification(
                reminder.getUserId(),
                "用药提醒：" + reminder.getMedicineName(),
                "到达服药时间，请按时服用 " + reminder.getDosage(),
                "MEDICINE_REMINDER"
            );
            // 更新下次提醒时间
            reminder.calculateNextRemindTime();
            reminderRepository.save(reminder);
        }
    }
}
```

### 5.8 NotificationService 改造

在现有 `NotificationService.sendNotification` 方法中，保存通知后增加推送逻辑：

```java
@Transactional
public void sendNotification(Integer userId, String title, String content, String type) {
    Notification notification = new Notification(userId, title, content, type);
    notification = notificationRepository.save(notification);

    NotificationPushMessage message = NotificationPushMessage.fromEntity(notification);
    message.setAction("NEW");
    webSocketPushService.pushToUser(userId, message);

    log.info("通知发送成功, 用户ID: {}, 标题: {}", userId, title);
}
```

### 5.9 状态同步广播

在标记已读/删除等操作后，同步推送给该用户所有在线端：

```java
@Transactional
public void markAsRead(Integer notificationId) {
    // ... 原有逻辑 ...
    webSocketPushService.syncNotificationState(user.getId(), "READ");
}

@Transactional
public void markAllAsRead() {
    // ... 原有逻辑 ...
    webSocketPushService.syncNotificationState(user.getId(), "READ_ALL");
}

@Transactional
public void deleteNotification(Integer notificationId) {
    // ... 原有逻辑 ...
    webSocketPushService.syncNotificationState(user.getId(), "DELETE");
}

@Transactional
public void deleteAllNotifications() {
    // ... 原有逻辑 ...
    webSocketPushService.syncNotificationState(user.getId(), "DELETE_ALL");
}
```

## 6. 前端设计

### 6.1 新增依赖

```bash
npm install @stomp/stompjs sockjs-client
```

### 6.2 WebSocket 客户端服务

新增 `src/utils/websocket.js`：

```javascript
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from './api'
import { useAuthStore } from '../stores/auth'

let stompClient = null
let reconnectTimer = null

export function connectWebSocket(onMessage) {
  const token = getToken()
  if (!token) return

  const socket = new SockJS('/api/v1/ws/notifications')
  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    debug: import.meta.env.DEV ? console.log : () => {},
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      // 订阅个人通知频道
      stompClient.subscribe('/user/queue/notifications', (message) => {
        const body = JSON.parse(message.body)
        if (onMessage) onMessage(body)
      })
      // 订阅管理员公告频道（仅管理员）
      const authStore = useAuthStore()
      if (authStore.isAdmin) {
        stompClient.subscribe('/topic/announcements', (message) => {
          const body = JSON.parse(message.body)
          if (onMessage) onMessage(body)
        })
      }
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame)
    }
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}
```

### 6.3 App.vue 集成

在 `App.vue` 中全局连接 WebSocket，收到消息时统一处理：

```javascript
import { onMounted, onUnmounted } from 'vue'
import { connectWebSocket, disconnectWebSocket } from './utils/websocket'
import { ElNotification } from 'element-plus'

function handleWebSocketMessage(message) {
  const authStore = useAuthStore()

  switch (message.action) {
    case 'NEW':
      // 更新未读数
      if (authStore.unreadCount !== undefined) {
        authStore.incrementUnreadCount()
      }
      // 右上角 Toast 提示
      ElNotification({
        title: message.title,
        message: message.content,
        type: message.type === 'EXPIRED_ALERT' ? 'warning' : 'info',
        position: 'top-right',
        duration: 5000
      })
      break
    case 'READ_ALL':
      authStore.setUnreadCount(0)
      break
    case 'DELETE_ALL':
      authStore.setUnreadCount(0)
      break
  }

  // 派发全局事件，通知 Notifications.vue 刷新列表
  window.dispatchEvent(new CustomEvent('notification:update', { detail: message }))
}

onMounted(() => {
  connectWebSocket(handleWebSocketMessage)
})

onUnmounted(() => {
  disconnectWebSocket()
})
```

### 6.4 Navbar 未读数显示

在 `Navbar.vue` 中显示未读通知数量：

```html
<router-link to="/notifications" class="nav-link">
  <el-icon><Bell /></el-icon>
  <span>消息通知</span>
  <el-badge v-if="unreadCount > 0" :value="unreadCount" class="notification-badge" />
</router-link>
```

### 6.5 Notifications.vue 实时刷新

监听 `notification:update` 事件，根据 action 类型更新列表和统计：

```javascript
onMounted(() => {
  fetchNotifications()
  fetchStats()
  window.addEventListener('notification:update', handleNotificationUpdate)
})

onUnmounted(() => {
  window.removeEventListener('notification:update', handleNotificationUpdate)
})

function handleNotificationUpdate(e) {
  const message = e.detail
  switch (message.action) {
    case 'NEW':
      notifications.value.unshift(message)
      if (stats.value) stats.value.totalCount++
      break
    case 'READ':
    case 'READ_ALL':
      fetchNotifications()
      fetchStats()
      break
    case 'DELETE':
    case 'DELETE_ALL':
      fetchNotifications()
      fetchStats()
      break
  }
}
```

## 7. 触发点集成

### 7.1 用药提醒
- `ReminderScheduler` 定时扫描到期提醒
- 调用 `NotificationService.sendNotification`

### 7.2 库存/过期预警
- 在 `MedicineService` 中保存药品后，检查库存和有效期
- 如果触发预警条件，调用 `NotificationService.sendNotification`

### 7.3 管理员公告
- 新增管理员接口 `POST /api/v1/admin/announcements`
- 调用 `NotificationService.sendAnnouncementToAll(title, content)`
- 该方法遍历所有活跃用户，每人写入一条通知，并广播 `topic/announcements`

### 7.4 账号状态变更
- 在 `AuthService.blockUser/freezeUser/unblockUser` 中
- 状态变更后调用 `NotificationService.sendNotification(userId, title, content, "ACCOUNT")`

## 8. 错误处理

### 8.1 后端
- WebSocket 认证失败时拒绝连接，前端收到错误后不重试，等待用户重新登录
- 推送失败不影响通知入库，保证数据不丢失
- 定时任务异常捕获并记录日志，不中断后续扫描

### 8.2 前端
- WebSocket 断线后自动重连（stompjs 内置 reconnectDelay）
- Token 过期后收到 401，触发 `api:unauthorized` 事件跳转登录页
- WebSocket 连接失败时回退到现有轮询机制（可选，首次实现可不启用）

## 9. 测试策略

### 9.1 后端单元测试
- `WebSocketAuthInterceptor`：验证 JWT 正确/错误/缺失场景
- `WebSocketPushService`：验证 `convertAndSendToUser` 调用
- `ReminderScheduler`：验证到期提醒扫描和推送

### 9.2 集成测试
- 启动后端 + 前端，登录后观察通知页面
- 手动调用管理员接口发布公告，验证多端同时收到
- 修改药品库存触发预警，验证实时推送

### 9.3 性能测试
- 模拟 100 个并发 WebSocket 连接，验证服务器稳定性
- 检查 30 秒定时任务在大数据量下的性能

## 10. 部署注意事项

### 10.1 Nginx 反向代理
如果使用 Nginx，需要添加 WebSocket 支持：

```nginx
location /api/v1/ws/ {
    proxy_pass http://localhost:8080/api/v1/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 10.2 生产环境建议
- 使用外置消息代理（如 RabbitMQ / ActiveMQ）替代内存 broker，支持集群部署
- 启用 STOMP over WebSocket 的心跳和断线检测
- 限制单个用户最大 WebSocket 连接数，防止资源耗尽

## 11. 实施计划概要

1. 后端：添加 `spring-boot-starter-websocket` 依赖
2. 后端：新增 WebSocket 配置、JWT 认证拦截器、Principal、安全配置
3. 后端：新增 `WebSocketPushService`
4. 后端：改造 `NotificationService`，入库后推送
5. 后端：新增用药提醒定时任务 `ReminderScheduler`
6. 后端：在药品/管理员/用户状态变更处集成通知触发
7. 前端：安装 `@stomp/stompjs` 和 `sockjs-client`
8. 前端：新增 `websocket.js` 客户端服务
9. 前端：在 `App.vue` 中全局连接 WebSocket
10. 前端：在 `Navbar.vue` 显示未读数
11. 前端：改造 `Notifications.vue` 实时刷新
12. 测试：单元测试 + 浏览器集成测试
