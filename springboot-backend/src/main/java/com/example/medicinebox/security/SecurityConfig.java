package com.example.medicinebox.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.medicinebox.dto.response.BaseResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security 安全配置类
 *
 * <p>配置应用的安全策略，包括：
 * <ul>
 *   <li>CSRF防护配置</li>
 *   <li>CORS跨域配置</li>
 *   <li>请求路径的权限控制规则</li>
 *   <li>无状态Session管理（JWT模式）</li>
 *   <li>认证入口点（未登录时的响应处理）</li>
 *   <li>JWT认证过滤器的注册</li>
 *   <li>密码编码器配置</li>
 * </ul>
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /** JWT认证过滤器，用于在每次请求前验证token */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 自定义用户详情服务，用于从数据库加载用户信息 */
    private final CustomUserDetailsService userDetailsService;

    /**
     * 构造函数，注入依赖的安全组件
     *
     * @param jwtAuthenticationFilter JWT认证过滤器
     * @param userDetailsService 自定义用户详情服务
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 配置HTTP安全策略
     *
     * <p>主要配置内容：
     * <ul>
     *   <li>禁用CSRF（使用JWT无状态认证，不需要CSRF保护）</li>
     *   <li>启用CORS跨域支持</li>
     *   <li>配置请求路径的访问权限：注册、登录、静态资源、健康检查等接口放行</li>
     *   <li>actuator端点需要ADMIN角色权限</li>
     *   <li>其余所有请求都需要认证</li>
     *   <li>配置无状态Session策略（不创建HttpSession）</li>
     *   <li>配置认证失败时的入口点（返回JSON格式的错误响应）</li>
     *   <li>在用户名密码认证过滤器之前添加JWT过滤器</li>
     * </ul>
     *
     * @param http HttpSecurity配置对象
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护，因为使用JWT无状态认证
                .csrf().disable()
                // 启用CORS跨域支持
                .cors()
                .and()
                // 配置请求授权规则
                .authorizeRequests()
                // 用户注册和登录接口放行，不需要认证
                .antMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/admin/login").permitAll()
                // 验证码接口放行，不需要认证
                .antMatchers("/api/v1/captcha/**").permitAll()
                // 静态资源和首页放行
                .antMatchers("/", "/index.html", "/static/**", "/*.html").permitAll()
                // 健康检查、信息、Prometheus指标端点放行
                .antMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                // 其他actuator端点需要管理员角色
                .antMatchers("/actuator/**").hasRole("ADMIN")
                // 其余所有请求都需要认证
                .anyRequest().authenticated()
                .and()
                // 配置无状态Session策略，完全依靠JWT token
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 配置认证异常处理入口点
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
                .and()
                // 在用户名密码认证过滤器之前添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * 配置认证管理器
     *
     * <p>设置使用自定义的UserDetailsService加载用户信息，
     * 并使用BCryptPasswordEncoder进行密码加密和验证。
     *
     * @param auth AuthenticationManagerBuilder认证管理器构建器
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    /**
     * 密码编码器Bean
     *
     * <p>使用BCrypt算法对密码进行加密存储和验证。
     * BCrypt是一种自适应哈希函数，内置盐值，安全性高。
     *
     * @return BCryptPasswordEncoder密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证入口点Bean
     *
     * <p>当用户未登录或token失效时，返回统一的JSON格式错误响应，
     * 而不是Spring Security默认的登录页面重定向。
     *
     * <p>返回的HTTP状态码为401 Unauthorized，响应体包含：
     * <ul>
     *   <li>code: 401</li>
     *   <li>status: error</li>
     *   <li>message: "未登录或token失效，请重新登录"</li>
     * </ul>
     *
     * @return AuthenticationEntryPoint认证入口点实例
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            // 设置响应内容类型为JSON
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // 设置字符编码为UTF-8
            response.setCharacterEncoding("UTF-8");
            // 设置HTTP状态码为401未授权
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // 构建统一的基础响应对象
            BaseResponse<Void> baseResponse = new BaseResponse<>(401, "error", "未登录或token失效，请重新登录", null);
            ObjectMapper mapper = new ObjectMapper();
            // 将响应对象序列化为JSON并写入响应体
            response.getWriter().write(mapper.writeValueAsString(baseResponse));
        };
    }

    /**
     * 认证管理器Bean
     *
     * <p>将AuthenticationManager暴露为Spring Bean，
     * 以便在其他地方（如登录Controller中）注入使用。
     *
     * @return AuthenticationManager认证管理器实例
     * @throws Exception 创建认证管理器时可能抛出的异常
     */
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
