package com.example.medicinebox.aspect;

import com.example.medicinebox.entity.SystemLog;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.service.SystemLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 系统日志切面
 *
 * <p>使用Spring AOP实现，自动记录所有Controller层方法的操作日志。
 * 环绕通知包裹目标方法执行，记录请求信息、用户信息、执行时间和执行结果。
 *
 * <p>日志记录内容：
 * <ul>
 *   <li>模块名称（根据URI自动识别）</li>
 *   <li>操作类型（根据HTTP方法和URI自动识别）</li>
 *   <li>请求方法（GET/POST/PUT/DELETE）</li>
 *   <li>请求URL</li>
 *   <li>客户端IP地址</li>
 *   <li>用户代理（User-Agent）</li>
 *   <li>操作用户手机号</li>
 *   <li>执行耗时（毫秒）</li>
 *   <li>是否成功</li>
 *   <li>错误信息（失败时）</li>
 * </ul>
 *
 * <p>切点：com.example.medicinebox.controller包下所有类的所有方法。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Aspect
@Component
public class SystemLogAspect {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(SystemLogAspect.class);

    /** 系统日志服务，用于保存日志到数据库 */
    private final SystemLogService systemLogService;

    /**
     * 构造函数，注入系统日志服务
     *
     * @param systemLogService 系统日志服务
     */
    public SystemLogAspect(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    /**
     * 定义切点：Controller层所有方法
     *
     * <p>切点表达式匹配com.example.medicinebox.controller包下
     * 所有类的所有public方法。
     */
    @Pointcut("execution(* com.example.medicinebox.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * 环绕通知：记录Controller方法的操作日志
     *
     * <p>在目标方法执行前后进行日志记录：
     * <ol>
     *   <li>获取HTTP请求对象，提取请求信息</li>
     *   <li>识别操作模块和操作类型</li>
     *   <li>获取当前登录用户信息（如果已登录）</li>
     *   <li>记录开始时间，执行目标方法</li>
     *   <li>方法成功执行：记录成功日志，包含耗时</li>
     *   <li>方法抛出异常：记录失败日志，包含错误信息</li>
     *   <li>异步保存日志到数据库（保存失败不影响主业务）</li>
     * </ol>
     *
     * @param joinPoint 连接点，包含目标方法信息
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = null;
        SystemLog systemLog = new SystemLog();
        // 记录方法开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 获取当前请求的属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
                // 根据URI提取模块名称
                String module = extractModule(request.getRequestURI());
                // 根据方法和URI提取操作类型
                String operation = extractOperation(request.getMethod(), request.getRequestURI());

                // 设置日志的模块和操作
                systemLog.setModule(module);
                systemLog.setOperation(operation);
                systemLog.setRequestMethod(request.getMethod());
                systemLog.setRequestUrl(request.getRequestURI());
                systemLog.setIpAddress(getClientIP(request));
                systemLog.setUserAgent(request.getHeader("User-Agent"));

                // 尝试获取当前登录用户信息
                try {
                    UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                            .getAuthentication().getPrincipal();
                    if (userDetails != null) {
                        systemLog.setUserPhone(userDetails.getUsername());
                    }
                } catch (Exception e) {
                    // 获取用户信息失败（可能未登录），记录debug日志
                    log.debug("获取用户信息失败: {}", e.getMessage());
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 计算方法执行耗时
            long duration = System.currentTimeMillis() - startTime;
            systemLog.setDurationMs(duration);
            systemLog.setSuccess(true);
            // 构建操作描述
            if (request != null) {
                systemLog.setDescription(buildDescription(systemLog.getOperation(), request));
            }

            // 保存日志（保存失败不影响主业务流程）
            try {
                systemLogService.saveLog(systemLog);
            } catch (Exception e) {
                log.error("保存系统日志失败: {}", e.getMessage());
            }

            return result;
        } catch (Throwable e) {
            // 方法执行异常
            long duration = System.currentTimeMillis() - startTime;
            systemLog.setDurationMs(duration);
            systemLog.setSuccess(false);
            systemLog.setErrorMessage(e.getMessage());
            // 构建操作描述（标记失败）
            if (request != null) {
                systemLog.setDescription(buildDescription(systemLog.getOperation(), request) + " - 失败");
            }

            // 保存日志（保存失败不影响主业务流程）
            try {
                systemLogService.saveLog(systemLog);
            } catch (Exception saveEx) {
                log.error("保存系统日志失败: {}", saveEx.getMessage());
            }

            // 重新抛出原始异常，让上层处理
            throw e;
        }
    }

    /**
     * 根据请求URI提取模块名称
     *
     * <p>通过URI中的关键词判断所属业务模块：
     * <ul>
     *   <li>/auth/ -> 用户认证</li>
     *   <li>/medicines/ -> 药品管理</li>
     *   <li>/prescription/ -> 药方管理</li>
     *   <li>/medication/ -> 服药提醒</li>
     *   <li>/device/ -> 设备指令</li>
     *   <li>/logs/ -> 系统日志</li>
     *   <li>其他 -> 系统管理</li>
     * </ul>
     *
     * @param uri 请求URI
     * @return 模块名称
     */
    private String extractModule(String uri) {
        if (uri.contains("/auth/")) return "用户认证";
        if (uri.contains("/medicines/")) return "药品管理";
        if (uri.contains("/prescription/")) return "药方管理";
        if (uri.contains("/medication/")) return "服药提醒";
        if (uri.contains("/device/")) return "设备指令";
        if (uri.contains("/logs/")) return "系统日志";
        return "系统管理";
    }

    /**
     * 根据HTTP方法和URI提取操作类型
     *
     * <p>结合HTTP方法和URI路径中的关键词，生成可读的操作描述：
     * <ul>
     *   <li>POST：注册、登录、添加药品、添加提醒、发送指令、创建</li>
     *   <li>GET：查看个人信息、查看用户列表、查看今日提醒、查看最近日志、查询</li>
     *   <li>PUT：修改密码、更新</li>
     *   <li>DELETE：删除</li>
     * </ul>
     *
     * @param method HTTP请求方法
     * @param uri 请求URI
     * @return 操作类型描述
     */
    private String extractOperation(String method, String uri) {
        if ("POST".equalsIgnoreCase(method)) {
            if (uri.contains("/register")) return "注册";
            if (uri.contains("/login")) return "登录";
            if (uri.contains("/medicines")) return "添加药品";
            if (uri.contains("/reminders")) return "添加提醒";
            if (uri.contains("/forward")) return "发送指令";
            return "创建";
        }
        if ("GET".equalsIgnoreCase(method)) {
            if (uri.contains("/me")) return "查看个人信息";
            if (uri.contains("/users")) return "查看用户列表";
            if (uri.contains("/today")) return "查看今日提醒";
            if (uri.contains("/recent")) return "查看最近日志";
            return "查询";
        }
        if ("PUT".equalsIgnoreCase(method)) {
            if (uri.contains("/change-password")) return "修改密码";
            return "更新";
        }
        if ("DELETE".equalsIgnoreCase(method)) return "删除";
        return method;
    }

    /**
     * 构建操作描述
     *
     * @param operation 操作类型
     * @param request HTTP请求对象
     * @return 操作描述字符串
     */
    private String buildDescription(String operation, HttpServletRequest request) {
        if (operation == null) operation = "未知操作";
        return operation + ": " + request.getRequestURI();
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>通过多个HTTP头依次尝试获取真实IP，处理反向代理场景：
     * <ol>
     *   <li>X-Forwarded-For（标准代理头）</li>
     *   <li>Proxy-Client-IP（Nginx代理）</li>
     *   <li>WL-Proxy-Client-IP（WebLogic代理）</li>
     *   <li>X-Real-IP（Nginx真实IP头）</li>
     *   <li>request.getRemoteAddr()（直连客户端IP）</li>
     * </ol>
     *
     * <p>对于X-Forwarded-For包含多个IP的情况（多级代理），
     * 取第一个IP作为真实客户端IP。
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
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
