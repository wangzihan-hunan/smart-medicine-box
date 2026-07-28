package com.example.medicinebox.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * <p>继承自OncePerRequestFilter，确保每个请求只执行一次过滤。
 * 从HTTP请求的Authorization头中提取JWT token，验证其有效性，
 * 并将认证信息设置到Spring Security的SecurityContext中。
 *
 * <p>过滤流程：
 * <ol>
 *   <li>白名单路径（登录、注册等）直接放行，不进行JWT验证</li>
 *   <li>从Authorization头中提取Bearer token</li>
 *   <li>解析token获取用户手机号</li>
 *   <li>从数据库加载用户详情</li>
 *   <li>验证token有效性</li>
 *   <li>验证通过则设置SecurityContext认证信息</li>
 *   <li>将脱敏后的手机号存入MDC以便日志追踪</li>
 * </ol>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** JWT工具类，用于token的解析和验证 */
    private final JwtUtil jwtUtil;

    /** 自定义用户详情服务，用于从数据库加载用户信息 */
    private final CustomUserDetailsService userDetailsService;

    /**
     * 构造函数，注入依赖组件
     *
     * @param jwtUtil JWT工具类
     * @param userDetailsService 自定义用户详情服务
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 执行JWT认证过滤逻辑
     *
     * <p>对每个HTTP请求进行JWT认证处理：
     * <ul>
     *   <li>登录、注册等白名单路径直接跳过认证</li>
     *   <li>从Authorization头中提取"Bearer "开头的token</li>
     *   <li>如果没有token或格式不正确，直接放行（由后续安全配置处理）</li>
     *   <li>解析token获取手机号，加载用户详情</li>
     *   <li>验证token有效后，将认证信息设置到SecurityContext</li>
     *   <li>认证异常时清除SecurityContext，保证安全</li>
     * </ul>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 获取请求URI，用于白名单判断
        String requestUri = request.getRequestURI();
        // 登录、注册等认证相关接口跳过JWT验证
        if (requestUri.contains("/auth/login") || requestUri.contains("/auth/register") ||
            requestUri.contains("/admin/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中获取Authorization字段
        final String authHeader = request.getHeader("Authorization");

        // 如果没有Authorization头或不是Bearer token，直接放行
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取JWT token（去掉"Bearer "前缀）
            final String jwt = authHeader.substring(7);
            // 从token中提取用户手机号
            final String phone = jwtUtil.extractPhone(jwt);

            // 如果手机号不为空且当前没有已认证的用户信息
            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 从数据库加载用户详情
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(phone);

                // 验证token有效性
                if (jwtUtil.validateToken(jwt)) {
                    // 创建认证令牌，包含用户详情、密码（null）和权限列表
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // 设置请求详情（IP、session等）
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // 将认证信息设置到SecurityContext中
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    // 对手机号进行脱敏处理
                    String maskedPhone = maskPhone(phone);
                    // 将脱敏后的手机号存入MDC，用于日志追踪
                    MDC.put("userPhone", maskedPhone);
                    log.debug("JWT认证成功, 用户手机号: {}", maskedPhone);
                } else {
                    log.warn("JWT Token验证失败");
                }
            }
        } catch (Exception e) {
            // JWT认证过程中出现任何异常，记录警告日志并清除安全上下文
            log.warn("JWT认证异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 手机号脱敏处理
     *
     * <p>将手机号中间4位替换为****，保护用户隐私。
     * 例如：13812345678 -> 138****5678
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
