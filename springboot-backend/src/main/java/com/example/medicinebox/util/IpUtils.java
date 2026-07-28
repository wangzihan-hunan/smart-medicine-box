package com.example.medicinebox.util;

import javax.servlet.http.HttpServletRequest;

/**
 * IP地址工具类
 *
 * <p>提供获取客户端真实IP地址的工具方法，
 * 处理反向代理、负载均衡等场景下的IP获取问题。
 *
 * <p>由于用户请求可能经过多级代理（Nginx、CDN等），
 * 需要依次从多个HTTP头中尝试获取真实客户端IP。
 *
 * @author medicine-box
 * @since 1.0.0
 */
public final class IpUtils {

    /**
     * 私有构造函数，禁止实例化工具类
     */
    private IpUtils() {
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>通过多个HTTP头依次尝试获取真实IP，处理反向代理场景：
     * <ol>
     *   <li>X-Forwarded-For：标准的代理转发头，可能包含多个IP（逗号分隔）</li>
     *   <li>Proxy-Client-IP：Nginx等代理设置的头</li>
     *   <li>WL-Proxy-Client-IP：WebLogic代理设置的头</li>
     *   <li>X-Real-IP：Nginx等代理设置的真实IP头</li>
     *   <li>request.getRemoteAddr()：直连客户端IP（最后兜底）</li>
     * </ol>
     *
     * <p>对于X-Forwarded-For包含多个IP的情况（多级代理），
     * 取第一个IP作为真实客户端IP（格式：client_ip, proxy1, proxy2, ...）。
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址字符串
     */
    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP为真实客户端IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
