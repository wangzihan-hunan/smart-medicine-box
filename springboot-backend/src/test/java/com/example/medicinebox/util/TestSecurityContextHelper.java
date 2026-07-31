package com.example.medicinebox.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/**
 * 测试用 SecurityContext 工具类
 * 统一管理 SecurityContext 的设置和清除，避免在多处重复模拟逻辑
 */
public final class TestSecurityContextHelper {

    private TestSecurityContextHelper() {
        // 工具类禁止实例化
    }

    /**
     * 设置当前用户的 SecurityContext（模拟已登录状态）
     *
     * @param phone    用户手机号
     * @param role     用户角色（USER / ADMIN）
     */
    public static void setCurrentUser(String phone, String role) {
        User userDetails = new User(phone, "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * 清除 SecurityContext，防止测试间状态污染
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }
}