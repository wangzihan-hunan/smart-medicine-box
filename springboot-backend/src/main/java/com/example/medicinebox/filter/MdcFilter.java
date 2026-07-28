package com.example.medicinebox.filter;

import com.example.medicinebox.util.IpUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * MDC（Mapped Diagnostic Context）过滤器
 *
 * <p>继承自OncePerRequestFilter，确保每个请求只执行一次。
 * 在请求处理前将请求相关信息存入SLF4J的MDC中，
 * 便于在日志中追踪每个请求的详细信息。
 *
 * <p>存入MDC的信息包括：
 * <ul>
 *   <li>requestId：请求唯一标识（UUID），用于链路追踪</li>
 *   <li>requestUri：请求URI</li>
 *   <li>requestMethod：HTTP请求方法</li>
 *   <li>clientIp：客户端IP地址</li>
 * </ul>
 *
 * <p>在日志格式中配置 %X{requestId} 等变量即可输出对应信息。
 * 请求处理完成后自动清除MDC，防止内存泄漏和线程复用导致的信息混乱。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class MdcFilter extends OncePerRequestFilter {

    /** MDC中请求唯一标识的key */
    private static final String REQUEST_ID_KEY = "requestId";
    /** MDC中请求URI的key */
    private static final String REQUEST_URI_KEY = "requestUri";
    /** MDC中请求方法的key */
    private static final String REQUEST_METHOD_KEY = "requestMethod";
    /** MDC中客户端IP的key */
    private static final String CLIENT_IP_KEY = "clientIp";

    /**
     * 执行MDC过滤逻辑
     *
     * <p>在请求处理前将请求相关信息存入MDC，
     * 请求处理完成后（finally块）清除MDC。
     *
     * <p>处理流程：
     * <ol>
     *   <li>生成唯一requestId（UUID，去除横杠）</li>
     *   <li>获取请求URI、请求方法、客户端IP</li>
     *   <li>将以上信息存入MDC</li>
     *   <li>继续过滤器链处理请求</li>
     *   <li>请求完成后清除MDC（finally块确保一定执行）</li>
     * </ol>
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
        try {
            // 生成唯一请求ID，用于链路追踪（去掉UUID中的横杠）
            MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().replace("-", ""));
            // 存入请求URI
            MDC.put(REQUEST_URI_KEY, request.getRequestURI());
            // 存入HTTP请求方法
            MDC.put(REQUEST_METHOD_KEY, request.getMethod());
            // 存入客户端IP地址
            MDC.put(CLIENT_IP_KEY, IpUtils.getClientIP(request));

            // 继续过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 请求处理完成后清除MDC，防止线程复用时信息混乱
            MDC.clear();
        }
    }
}
