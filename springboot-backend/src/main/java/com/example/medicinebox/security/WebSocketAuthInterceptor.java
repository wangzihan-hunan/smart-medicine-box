package com.example.medicinebox.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import com.example.medicinebox.util.PhoneUtils;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String phone = jwtUtil.extractPhone(token);
                    if (phone != null && jwtUtil.validateToken(token)) {
                        accessor.setUser(new StompPrincipal(phone));
                        log.debug("WebSocket 认证成功, 用户: {}", PhoneUtils.maskPhone(phone));
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
}
