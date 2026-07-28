# 通知实时推送功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为智能药箱系统添加基于 WebSocket + STOMP 的通知实时推送能力，支持用药提醒、库存预警、管理员公告、账号状态变更等场景。

**Architecture:** 在现有通知系统基础上增加 WebSocket 推送通道。所有通知先写入 `notifications` 表保证离线可见，同时通过 STOMP 推送给在线用户。前端通过 stompjs 订阅个人频道 `/user/queue/notifications`，收到消息后更新未读数并显示 Toast 提示。

**Tech Stack:** Spring Boot 2.7.x WebSocket + STOMP, Vue 3 + @stomp/stompjs + sockjs-client

---

## 文件结构

### 后端新增文件
- `springboot-backend/src/main/java/com/example/medicinebox/dto/response/NotificationPushMessage.java` - WebSocket 推送消息 DTO
- `springboot-backend/src/main/java/com/example/medicinebox/config/WebSocketConfig.java` - WebSocket 配置
- `springboot-backend/src/main/java/com/example/medicinebox/security/StompPrincipal.java` - STOMP 用户主体
- `springboot-backend/src/main/java/com/example/medicinebox/security/WebSocketAuthInterceptor.java` - JWT 握手认证拦截器
- `springboot-backend/src/main/java/com/example/medicinebox/config/WebSocketSecurityConfig.java` - WebSocket 安全配置
- `springboot-backend/src/main/java/com/example/medicinebox/service/WebSocketPushService.java` - WebSocket 推送服务
- `springboot-backend/src/main/java/com/example/medicinebox/scheduler/ReminderScheduler.java` - 用药提醒定时任务

### 后端修改文件
- `springboot-backend/pom.xml` - 添加 WebSocket 依赖
- `springboot-backend/src/main/java/com/example/medicinebox/service/NotificationService.java` - 入库后调用推送，状态同步广播
- `springboot-backend/src/main/java/com/example/medicinebox/entity/Reminder.java` - 添加 nextRemindTime 字段
- `springboot-backend/src/main/java/com/example/medicinebox/repository/ReminderRepository.java` - 添加查询方法

### 前端新增文件
- `qianduan/src/utils/websocket.js` - STOMP 客户端封装

### 前端修改文件
- `qianduan/package.json` - 添加依赖
- `qianduan/src/stores/auth.js` - 添加 unreadCount 状态和方法
- `qianduan/src/App.vue` - 全局连接 WebSocket 和消息处理
- `qianduan/src/components/Navbar.vue` - 显示未读数角标
- `qianduan/src/views/Notifications.vue` - 监听实时更新事件

---

## Task 1: 后端 - 添加 WebSocket 依赖

**Files:**
- Modify: `springboot-backend/pom.xml`

- [ ] **Step 1: 在 pom.xml 中添加 spring-boot-starter-websocket 依赖**

在 `<dependencies>` 标签内添加：

```xml
<!-- Spring Boot WebSocket + STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

插入位置建议：在 `spring-boot-starter-validation` 之后。

- [ ] **Step 2: 验证依赖可用**

运行命令：
```bash
cd springboot-backend
./mvnw dependency:resolve -q
```

预期输出：无错误，依赖解析成功。

---

## Task 2: 后端 - 创建推送消息 DTO

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/dto/response/NotificationPushMessage.java`

- [ ] **Step 1: 创建 NotificationPushMessage.java**

```java
package com.example.medicinebox.dto.response;

import com.example.medicinebox.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

/**
 * WebSocket 推送消息 DTO
 * 用于向客户端推送通知及其状态变更
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPushMessage {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 通知ID */
    private Integer id;
    /** 通知标题 */
    private String title;
    /** 通知内容 */
    private String content;
    /** 通知类型 */
    private String type;
    /** 是否已读 */
    private Boolean readStatus;
    /** 创建时间（ISO字符串） */
    private String createdAt;
    /** 动作类型：NEW / READ / READ_ALL / DELETE / DELETE_ALL */
    private String action;
    /** 当前未读数（可选，用于状态同步） */
    private Integer unreadCount;

    /**
     * 从 Notification 实体构建推送消息
     */
    public static NotificationPushMessage fromEntity(Notification notification) {
        NotificationPushMessage message = new NotificationPushMessage();
        message.setId(notification.getId());
        message.setTitle(notification.getTitle());
        message.setContent(notification.getContent());
        message.setType(notification.getType());
        message.setReadStatus(notification.getReadStatus());
        if (notification.getCreatedAt() != null) {
            message.setCreatedAt(notification.getCreatedAt().format(FORMATTER));
        }
        return message;
    }
}
```

- [ ] **Step 2: 验证编译通过**

运行命令：
```bash
cd springboot-backend
./mvnw compile -q
```

预期输出：BUILD SUCCESS。

---

## Task 3: 后端 - 创建 STOMP Principal

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/security/StompPrincipal.java`

- [ ] **Step 1: 创建 StompPrincipal.java**

```java
package com.example.medicinebox.security;

import java.security.Principal;

/**
 * STOMP 用户主体实现
 * 用于在 WebSocket 会话中标识已认证用户
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
```

---

## Task 4: 后端 - 创建 WebSocket 配置

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/config/WebSocketConfig.java`

- [ ] **Step 1: 创建 WebSocketConfig.java**

```java
package com.example.medicinebox.config;

import com.example.medicinebox.security.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置类
 * 启用消息代理，配置端点和消息前缀
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * 配置消息代理
     * /topic: 广播消息前缀
     * /queue: 点对点消息前缀
     * /app: 客户端发送消息前缀
     * /user: 用户目的地前缀
     */
    @Override
    public void configureMessageBroker(org.springframework.messaging.simp.config.MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点
     * 客户端通过 /api/v1/ws/notifications 连接 WebSocket
     * 使用 SockJS 作为降级方案
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 配置客户端入站通道拦截器
     * 在 STOMP CONNECT 时进行 JWT 认证
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
```

---

## Task 5: 后端 - 创建 JWT 握手认证拦截器

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/security/WebSocketAuthInterceptor.java`

- [ ] **Step 1: 创建 WebSocketAuthInterceptor.java**

```java
package com.example.medicinebox.security;

import com.example.medicinebox.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 握手认证拦截器
 * 在 STOMP CONNECT 阶段验证 JWT token，设置用户主体
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // 仅在 CONNECT 命令时进行认证
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String phone = jwtUtil.extractPhone(token);
                    if (phone != null && jwtUtil.validateToken(token)) {
                        // 设置用户主体，后续消息路由使用
                        accessor.setUser(new StompPrincipal(phone));
                        log.debug("WebSocket 认证成功, 用户: {}", maskPhone(phone));
                    } else {
                        log.warn("WebSocket 认证失败: token 无效");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket 认证异常: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket 连接缺少 Authorization 头");
            }
        }

        return message;
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
```

---

## Task 6: 后端 - 创建 WebSocket 安全配置

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/config/WebSocketSecurityConfig.java`

- [ ] **Step 1: 创建 WebSocketSecurityConfig.java**

```java
package com.example.medicinebox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * WebSocket 安全配置
 * 定义消息目的地的访问权限
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // 客户端发送到 /app/** 的消息需要认证
            .simpDestMatchers("/app/**").authenticated()
            // 订阅个人通知频道需要认证
            .simpSubscribeDestMatchers("/user/queue/notifications").authenticated()
            // 订阅管理员公告频道需要 ADMIN 角色
            .simpSubscribeDestMatchers("/topic/announcements").hasRole("ADMIN")
            // 其他所有消息拒绝
            .anyMessage().denyAll();
    }

    /**
     * 禁用同源检查，允许跨域 WebSocket 连接
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
```

---

## Task 7: 后端 - 创建 WebSocket 推送服务

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/service/WebSocketPushService.java`

- [ ] **Step 1: 创建 WebSocketPushService.java**

```java
package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.NotificationPushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 推送服务
 * 封装 STOMP 消息推送逻辑
 */
@Service
public class WebSocketPushService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPushService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 向指定用户推送通知
     *
     * @param userId 用户ID
     * @param message 推送消息对象
     */
    public void pushToUser(Integer userId, NotificationPushMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                message
            );
            log.debug("推送通知到用户 {}, action: {}", userId, message.getAction());
        } catch (Exception e) {
            log.error("推送通知失败, 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    /**
     * 向所有在线用户广播公告
     *
     * @param message 推送消息对象
     */
    public void broadcastAnnouncement(NotificationPushMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/announcements", message);
            log.info("广播公告: {}", message.getTitle());
        } catch (Exception e) {
            log.error("广播公告失败: {}", e.getMessage());
        }
    }

    /**
     * 推送状态同步指令
     *
     * @param userId 用户ID
     * @param action 动作类型（READ / READ_ALL / DELETE / DELETE_ALL）
     */
    public void syncNotificationState(Integer userId, String action) {
        NotificationPushMessage message = new NotificationPushMessage();
        message.setAction(action);
        pushToUser(userId, message);
    }
}
```

---

## Task 8: 后端 - 创建用药提醒定时任务

**Files:**
- Create: `springboot-backend/src/main/java/com/example/medicinebox/scheduler/ReminderScheduler.java`

- [ ] **Step 1: 创建 ReminderScheduler.java**

```java
package com.example.medicinebox.scheduler;

import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用药提醒定时任务
 * 每 30 秒扫描一次到期提醒并发送通知
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每 30 秒扫描一次到期提醒
     */
    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void checkDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("扫描到期提醒, 当前时间: {}", now);

        // 查询已到期且未发送通知的活跃提醒
        List<Reminder> dueReminders = reminderRepository.findByIsActiveTrueAndNextRemindTimeLessThanEqual(now);

        if (dueReminders.isEmpty()) {
            return;
        }

        log.info("发现 {} 条到期提醒", dueReminders.size());

        for (Reminder reminder : dueReminders) {
            try {
                // 发送通知
                notificationService.sendNotification(
                    reminder.getUserId(),
                    "用药提醒：" + reminder.getMedicineName(),
                    "到达服药时间，请按时服用 " + reminder.getDosage(),
                    "MEDICINE_REMINDER"
                );

                // 更新下次提醒时间（如果启用了提前提醒，会相应处理）
                updateNextRemindTime(reminder);
                
                log.debug("提醒已发送, 用户ID: {}, 药品: {}", reminder.getUserId(), reminder.getMedicineName());
            } catch (Exception e) {
                log.error("发送提醒失败, 提醒ID: {}, 错误: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    /**
     * 更新下次提醒时间
     * 简单实现：直接标记为已处理，后续可根据频率计算下次时间
     */
    private void updateNextRemindTime(Reminder reminder) {
        // 标记为已提醒，避免重复发送
        reminder.setNextRemindTime(LocalDateTime.now().plusYears(100));
        reminderRepository.save(reminder);
    }
}
```

---

## Task 9: 后端 - 扩展 ReminderRepository

**Files:**
- Modify: `springboot-backend/src/main/java/com/example/medicinebox/repository/ReminderRepository.java`

- [ ] **Step 1: 在 ReminderRepository 中添加查询方法**

找到 `ReminderRepository.java` 文件，在接口中添加方法：

```java
/**
 * 查询已到期且活跃的提醒
 */
List<Reminder> findByIsActiveTrueAndNextRemindTimeLessThanEqual(LocalDateTime time);
```

如果接口已有其他方法，将此方法添加到末尾。

---

## Task 10: 后端 - 扩展 Reminder 实体

**Files:**
- Modify: `springboot-backend/src/main/java/com/example/medicinebox/entity/Reminder.java`

- [ ] **Step 1: 在 Reminder 实体中添加 nextRemindTime 字段**

找到 `Reminder.java` 文件，在字段区域添加：

```java
/** 下次提醒时间（用于定时任务扫描） */
@Column(name = "next_remind_time")
private LocalDateTime nextRemindTime;
```

在构造函数或 onCreate 方法中初始化：

```java
// 在 @PrePersist 方法中添加
if (nextRemindTime == null) {
    // 默认设置为创建时间（实际应根据 frequency 计算）
    nextRemindTime = LocalDateTime.now();
}
```

---

## Task 11: 后端 - 改造 NotificationService

**Files:**
- Modify: `springboot-backend/src/main/java/com/example/medicinebox/service/NotificationService.java`

- [ ] **Step 1: 在 NotificationService 中注入 WebSocketPushService**

在类的字段区域添加：

```java
@Autowired
private WebSocketPushService webSocketPushService;
```

- [ ] **Step 2: 改造 sendNotification 方法**

找到 `sendNotification` 方法，在保存通知后添加推送逻辑：

```java
@Transactional
public void sendNotification(Integer userId, String title, String content, String type) {
    log.debug("发送通知, 用户ID: {}, 标题: {}", userId, title);
    Notification notification = new Notification(userId, title, content, type);
    notificationRepository.save(notification);
    log.info("通知已保存, 用户ID: {}, 标题: {}", userId, title);

    // 实时推送
    NotificationPushMessage message = NotificationPushMessage.fromEntity(notification);
    message.setAction("NEW");
    webSocketPushService.pushToUser(userId, message);
}
```

- [ ] **Step 3: 在 markAsRead 方法中添加状态同步**

找到 `markAsRead` 方法，在保存后添加：

```java
webSocketPushService.syncNotificationState(user.getId(), "READ");
```

- [ ] **Step 4: 在 markAllAsRead 方法中添加状态同步**

找到 `markAllAsRead` 方法，在批量更新后添加：

```java
webSocketPushService.syncNotificationState(user.getId(), "READ_ALL");
```

- [ ] **Step 5: 在 deleteNotification 方法中添加状态同步**

找到 `deleteNotification` 方法，在删除后添加：

```java
webSocketPushService.syncNotificationState(user.getId(), "DELETE");
```

- [ ] **Step 6: 在 deleteAllNotifications 方法中添加状态同步**

找到 `deleteAllNotifications` 方法，在删除后添加：

```java
webSocketPushService.syncNotificationState(user.getId(), "DELETE_ALL");
```

---

## Task 12: 后端 - 启用定时任务

**Files:**
- Modify: `springboot-backend/src/main/java/com/example/medicinebox/MedicineBoxApplication.java`

- [ ] **Step 1: 在主类上添加 @EnableScheduling 注解**

找到 `MedicineBoxApplication.java`，在类声明上方添加注解：

```java
@SpringBootApplication
@EnableScheduling  // 新增
public class MedicineBoxApplication {
    // ...
}
```

---

## Task 13: 后端 - 验证编译和启动

- [ ] **Step 1: 编译后端项目**

运行命令：
```bash
cd springboot-backend
./mvnw clean compile
```

预期输出：BUILD SUCCESS，无编译错误。

- [ ] **Step 2: 启动后端服务**

运行命令：
```bash
cd springboot-backend
./mvnw spring-boot:run
```

预期输出：应用启动成功，端口 8080，WebSocket 端点 `/api/v1/ws/notifications` 可访问。

---

## Task 14: 前端 - 安装 WebSocket 依赖

**Files:**
- Modify: `qianduan/package.json`

- [ ] **Step 1: 安装 @stomp/stompjs 和 sockjs-client**

运行命令：
```bash
cd qianduan
npm install @stomp/stompjs sockjs-client
```

预期输出：依赖安装成功，package.json 和 package-lock.json 已更新。

---

## Task 15: 前端 - 创建 WebSocket 客户端服务

**Files:**
- Create: `qianduan/src/utils/websocket.js`

- [ ] **Step 1: 创建 websocket.js**

```javascript
/**
 * WebSocket + STOMP 客户端封装
 * 用于连接服务器推送通知
 */
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from './api'
import { useAuthStore } from '../stores/auth'

let stompClient = null

/**
 * 连接 WebSocket 并订阅通知频道
 * @param {Function} onMessage 收到消息时的回调函数
 */
export function connectWebSocket(onMessage) {
  const token = getToken()
  if (!token) {
    console.warn('[WebSocket] 无 token，跳过连接')
    return
  }

  // 如果已连接，先断开
  if (stompClient) {
    disconnectWebSocket()
  }

  const socket = new SockJS('/api/v1/ws/notifications')

  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    debug: import.meta.env.DEV ? (str) => console.log('[STOMP]', str) : () => {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[WebSocket] 已连接')

      // 订阅个人通知频道
      stompClient.subscribe('/user/queue/notifications', (message) => {
        try {
          const body = JSON.parse(message.body)
          if (onMessage) onMessage(body)
        } catch (e) {
          console.error('[WebSocket] 解析消息失败:', e)
        }
      })

      // 订阅管理员公告频道（仅管理员）
      const authStore = useAuthStore()
      if (authStore.isAdmin) {
        stompClient.subscribe('/topic/announcements', (message) => {
          try {
            const body = JSON.parse(message.body)
            if (onMessage) onMessage(body)
          } catch (e) {
            console.error('[WebSocket] 解析公告失败:', e)
          }
        })
      }
    },
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP 错误:', frame)
    },
    onWebSocketError: (event) => {
      console.error('[WebSocket] 连接错误:', event)
    },
    onDisconnect: () => {
      console.log('[WebSocket] 已断开')
    }
  })

  stompClient.activate()
}

/**
 * 断开 WebSocket 连接
 */
export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

/**
 * 检查 WebSocket 是否已连接
 */
export function isWebSocketConnected() {
  return stompClient && stompClient.connected
}
```

---

## Task 16: 前端 - 扩展 auth store

**Files:**
- Modify: `qianduan/src/stores/auth.js`

- [ ] **Step 1: 在 auth store 中添加 unreadCount 状态**

在 `useAuthStore` 函数内部添加状态和方法：

```javascript
// 未读通知数
const unreadCount = ref(0)

// 增加未读数
function incrementUnreadCount() {
  unreadCount.value++
}

// 减少未读数
function decrementUnreadCount() {
  if (unreadCount.value > 0) {
    unreadCount.value--
  }
}

// 设置未读数
function setUnreadCount(count) {
  unreadCount.value = count
}

// 重置未读数（登出时调用）
function resetUnreadCount() {
  unreadCount.value = 0
}
```

在 return 语句中导出：

```javascript
return {
  token,
  user,
  isLoggedIn,
  isAdmin,
  unreadCount,
  login,
  fetchUserInfo,
  logout,
  incrementUnreadCount,
  decrementUnreadCount,
  setUnreadCount,
  resetUnreadCount
}
```

- [ ] **Step 2: 在 logout 方法中重置未读数**

修改 `logout` 函数：

```javascript
function logout() {
  token.value = null
  user.value = null
  unreadCount.value = 0  // 新增
}
```

---

## Task 17: 前端 - 在 App.vue 中全局连接 WebSocket

**Files:**
- Modify: `qianduan/src/App.vue`

- [ ] **Step 1: 导入 WebSocket 工具和 Element Plus 通知组件**

在 `<script setup>` 顶部添加：

```javascript
import { connectWebSocket, disconnectWebSocket } from './utils/websocket'
import { ElNotification } from 'element-plus'
```

- [ ] **Step 2: 添加 WebSocket 消息处理函数**

在 `<script setup>` 中添加：

```javascript
// 处理 WebSocket 推送的消息
function handleWebSocketMessage(message) {
  const authStore = useAuthStore()

  switch (message.action) {
    case 'NEW':
      // 更新未读数
      authStore.incrementUnreadCount()
      // 右上角 Toast 提示
      ElNotification({
        title: message.title || '新通知',
        message: message.content || '',
        type: message.type === 'EXPIRED_ALERT' ? 'warning' : 'info',
        position: 'top-right',
        duration: 5000
      })
      // 派发全局事件，通知 Notifications.vue 刷新列表
      window.dispatchEvent(new CustomEvent('notification:update', { detail: message }))
      break
    case 'READ_ALL':
      authStore.setUnreadCount(0)
      window.dispatchEvent(new CustomEvent('notification:update', { detail: message }))
      break
    case 'DELETE_ALL':
      authStore.setUnreadCount(0)
      window.dispatchEvent(new CustomEvent('notification:update', { detail: message }))
      break
    default:
      // 其他状态同步操作，通知页面刷新
      window.dispatchEvent(new CustomEvent('notification:update', { detail: message }))
  }
}
```

- [ ] **Step 3: 在 onMounted 中连接 WebSocket**

修改 `onMounted` 钩子：

```javascript
onMounted(() => {
  document.body.classList.add('app-loaded')
  window.addEventListener('api:unauthorized', handleUnauthorized)
  // 连接 WebSocket
  if (authStore.isLoggedIn) {
    connectWebSocket(handleWebSocketMessage)
  }
})
```

- [ ] **Step 4: 在 onUnmounted 中断开 WebSocket**

修改 `onUnmounted` 钩子：

```javascript
onUnmounted(() => {
  document.body.classList.remove('app-loaded')
  window.removeEventListener('api:unauthorized', handleUnauthorized)
  // 断开 WebSocket
  disconnectWebSocket()
})
```

- [ ] **Step 5: 监听登录状态变化，自动连接/断开 WebSocket**

在 `<script setup>` 中添加 watch：

```javascript
import { watch } from 'vue'

// 监听登录状态，自动连接/断开 WebSocket
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    connectWebSocket(handleWebSocketMessage)
  } else {
    disconnectWebSocket()
  }
})
```

---

## Task 18: 前端 - 在 Navbar 显示未读数

**Files:**
- Modify: `qianduan/src/components/Navbar.vue`

- [ ] **Step 1: 在通知导航链接中添加未读数角标**

找到通知导航链接的 `<router-link>` 部分，修改为：

```html
<router-link to="/notifications" class="nav-link" :class="{ 'nav-link-active': $route.path === '/notifications' }" @click="isMenuOpen = false">
  <el-icon class="nav-icon"><Bell /></el-icon>
  <span class="nav-text">消息通知</span>
  <el-badge v-if="authStore.unreadCount > 0" :value="authStore.unreadCount" :max="99" class="notification-badge" />
</router-link>
```

- [ ] **Step 2: 在 style 中添加角标样式**

在 `<style scoped>` 中添加：

```css
/* 通知未读数角标 */
.notification-badge {
  margin-left: 4px;
}

.notification-badge .el-badge__content {
  font-size: 10px;
  height: 16px;
  line-height: 16px;
  padding: 0 5px;
}
```

- [ ] **Step 3: 在 onMounted 中获取未读数**

修改 `onMounted` 钩子，在 `fetchUser()` 后添加：

```javascript
onMounted(() => {
  fetchUser()
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
  // 获取未读通知数
  fetchUnreadCount()
})
```

添加获取未读数的函数：

```javascript
import { notificationAPI } from '../utils/api'

// 获取未读通知数
async function fetchUnreadCount() {
  if (!authStore.isLoggedIn) return
  try {
    const stats = await notificationAPI.getNotificationStats()
    if (stats && stats.unreadCount !== undefined) {
      authStore.setUnreadCount(stats.unreadCount)
    }
  } catch (error) {
    console.error('获取未读数失败:', error)
  }
}
```

---

## Task 19: 前端 - 改造 Notifications.vue 实时刷新

**Files:**
- Modify: `qianduan/src/views/Notifications.vue`

- [ ] **Step 1: 监听 notification:update 事件**

在 `<script setup>` 的 `onMounted` 和 `onUnmounted` 中添加事件监听：

```javascript
onMounted(() => {
  fetchNotifications()
  fetchStats()
  // 监听实时更新事件
  window.addEventListener('notification:update', handleNotificationUpdate)
})

onUnmounted(() => {
  window.removeEventListener('notification:update', handleNotificationUpdate)
})
```

- [ ] **Step 2: 添加事件处理函数**

在 `<script setup>` 中添加：

```javascript
// 处理实时通知更新事件
function handleNotificationUpdate(e) {
  const message = e.detail
  switch (message.action) {
    case 'NEW':
      // 新增通知，插入到列表头部
      if (message.id) {
        notifications.value.unshift(message)
        if (stats.value) stats.value.totalCount++
      }
      break
    case 'READ':
    case 'READ_ALL':
      // 标记已读，刷新列表和统计
      fetchNotifications()
      fetchStats()
      break
    case 'DELETE':
    case 'DELETE_ALL':
      // 删除通知，刷新列表和统计
      fetchNotifications()
      fetchStats()
      break
    default:
      // 其他情况，刷新数据
      fetchNotifications()
      fetchStats()
  }
}
```

- [ ] **Step 3: 在 fetchStats 成功后更新 authStore 的未读数**

修改 `fetchStats` 函数：

```javascript
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

async function fetchStats() {
  try {
    const data = await notificationAPI.getNotificationStats()
    stats.value = data || null
    // 同步未读数到 authStore
    if (data && data.unreadCount !== undefined) {
      authStore.setUnreadCount(data.unreadCount)
    }
  } catch (error) {
    console.error('获取通知统计失败:', error)
  }
}
```

---

## Task 20: 前端 - 验证构建

- [ ] **Step 1: 构建前端项目**

运行命令：
```bash
cd qianduan
npm run build
```

预期输出：构建成功，无错误。

- [ ] **Step 2: 启动前端开发服务器**

运行命令：
```bash
cd qianduan
npm run dev
```

预期输出：Vite 开发服务器启动在 http://localhost:3002/。

---

## Task 21: 集成测试

- [ ] **Step 1: 启动后端服务**

运行命令：
```bash
cd springboot-backend
./mvnw spring-boot:run
```

- [ ] **Step 2: 启动前端服务**

运行命令：
```bash
cd qianduan
npm run dev
```

- [ ] **Step 3: 测试 WebSocket 连接**

1. 打开浏览器访问 http://localhost:3002/login
2. 登录用户账号
3. 打开浏览器开发者工具 → Network → WS（WebSocket）
4. 观察是否有 WebSocket 连接建立到 `/api/v1/ws/notifications`
5. 查看控制台是否有 `[WebSocket] 已连接` 日志

- [ ] **Step 4: 测试通知推送**

1. 登录后打开两个浏览器标签页
2. 在第一个标签页访问通知页面
3. 在第二个标签页执行一个会触发通知的操作（如修改药品库存触发预警）
4. 观察第一个标签页是否实时显示新通知（右上角 Toast + 列表刷新）

- [ ] **Step 5: 测试多端同步**

1. 在两个标签页都打开通知页面
2. 在第一个标签页点击"全部标为已读"
3. 观察第二个标签页是否同步更新未读数为 0

---

## 自检清单

完成所有任务后，请确认：

- [ ] 后端启动成功，WebSocket 端点 `/api/v1/ws/notifications` 可访问
- [ ] 前端启动成功，登录后自动连接 WebSocket
- [ ] 收到新通知时右上角显示 Toast 提示
- [ ] 通知列表实时刷新，无需手动刷新页面
- [ ] 多个标签页之间状态同步
- [ ] 导航栏显示未读通知数角标
- [ ] 用药提醒定时任务每 30 秒扫描一次

---

## 回退方案

如果实施过程中遇到问题，可以按以下步骤回退：

1. **前端回退**：
   - 删除 `qianduan/src/utils/websocket.js`
   - 移除 `App.vue` 中的 WebSocket 相关代码
   - 移除 `Navbar.vue` 中的角标代码
   - 移除 `Notifications.vue` 中的事件监听
   - 恢复 `package.json`（删除 @stomp/stompjs 和 sockjs-client）

2. **后端回退**：
   - 移除 `pom.xml` 中的 WebSocket 依赖
   - 删除 `WebSocketConfig.java`
   - 删除 `WebSocketAuthInterceptor.java`
   - 删除 `StompPrincipal.java`
   - 删除 `WebSocketSecurityConfig.java`
   - 删除 `WebSocketPushService.java`
   - 删除 `ReminderScheduler.java`
   - 恢复 `NotificationService.java` 中的推送调用
   - 移除 `MedicineBoxApplication.java` 上的 @EnableScheduling