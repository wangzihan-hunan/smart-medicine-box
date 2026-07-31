package com.example.medicinebox.service;

import com.example.medicinebox.dto.response.SystemLogListResponse;
import com.example.medicinebox.dto.response.SystemLogResponse;
import com.example.medicinebox.entity.SystemLog;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.util.PhoneUtils;
import com.example.medicinebox.repository.SystemLogRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.IpUtils;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统日志服务类
 * 核心功能：系统操作日志记录（登录/注册/业务操作）、日志分页查询、日志统计等
 * 日志保存采用异步方式，避免影响主业务性能
 */
@Service
public class SystemLogService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogService.class);

    private final SystemLogRepository systemLogRepository;

    /**
     * 构造函数，注入依赖
     *
     * @param systemLogRepository 系统日志数据访问层
     * @param userRepository 用户数据访问层
     */
    public SystemLogService(SystemLogRepository systemLogRepository, UserRepository userRepository) {
        super(userRepository);
        this.systemLogRepository = systemLogRepository;
    }

    /**
     * 异步保存系统日志（新事务，独立于主事务）
     * 日志保存失败不影响主业务，仅记录错误日志
     *
     * @param systemLog 系统日志实体
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
        } catch (Exception e) {
            log.error("保存系统日志失败: {}", e.getMessage());
        }
    }

    /**
     * 记录登录成功日志
     *
     * @param phone 用户手机号
     * @param ipAddress IP地址
     */
    @Transactional
    public void recordLoginSuccess(String phone, String ipAddress) {
        User user = userRepository.findByPhone(phone).orElse(null);
        
        SystemLog log = new SystemLog();
        log.setModule("用户认证");
        log.setOperation("登录成功");
        log.setDescription("用户登录成功");
        log.setIpAddress(ipAddress);
        
        if (user != null) {
            log.setUserId(user.getId());
            log.setUserName(user.getName());
            log.setUserPhone(PhoneUtils.maskPhone(user.getPhone()));
        }

        saveLog(log);
    }

    /**
     * 记录登录失败日志
     *
     * @param phone 用户手机号
     * @param ipAddress IP地址
     * @param error 错误信息
     */
    @Transactional
    public void recordLoginFailure(String phone, String ipAddress, String error) {
        SystemLog log = new SystemLog();
        log.setModule("用户认证");
        log.setOperation("登录失败");
        log.setDescription("用户登录失败: " + error);
        log.setUserPhone(PhoneUtils.maskPhone(phone));
        log.setIpAddress(ipAddress);
        log.setSuccess(false);
        log.setErrorMessage(error);

        saveLog(log);
    }

    /**
     * 记录用户注册日志
     *
     * @param phone 手机号
     * @param name 姓名
     */
    @Transactional
    public void recordRegister(String phone, String name) {
        User user = userRepository.findByPhone(phone).orElse(null);

        SystemLog log = new SystemLog();
        log.setModule("用户认证");
        log.setOperation("注册");
        log.setDescription("用户注册: " + PhoneUtils.maskPhone(phone));

        if (user != null) {
            log.setUserId(user.getId());
            log.setUserName(user.getName());
            log.setUserPhone(PhoneUtils.maskPhone(user.getPhone()));
        } else {
            log.setUserPhone(PhoneUtils.maskPhone(phone));
        }

        saveLog(log);
    }

    /**
     * 记录业务操作日志
     * 记录模块、操作、描述、请求信息和用户信息
     *
     * @param module 模块名称
     * @param operation 操作名称
     * @param description 操作描述
     * @param request HTTP请求对象（用于获取请求信息）
     */
    @Transactional
    public void recordOperation(String module, String operation, String description, 
                                HttpServletRequest request) {
        User user = null;
        try {
            user = getCurrentUser();
        } catch (Exception e) {
            // 未登录用户不记录用户信息
        }
        
        SystemLog log = new SystemLog();
        log.setModule(module);
        log.setOperation(operation);
        log.setDescription(description);
        log.setRequestMethod(request.getMethod());
        log.setRequestUrl(request.getRequestURI());
        log.setIpAddress(IpUtils.getClientIP(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        
        // 已登录用户记录用户信息
        if (user != null) {
            log.setUserId(user.getId());
            log.setUserName(user.getName());
            log.setUserPhone(user.getPhone());
        }
        
        saveLog(log);
    }

    /**
     * 获取最新的10条系统日志
     *
     * @return 系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getTop10Logs() {
        Page<SystemLog> page = systemLogRepository.findTop10ByOrderByCreatedAtDesc(PageRequest.of(0, 10));
        return SystemLogListResponse.of(page.getContent().stream()
                .map(SystemLogResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * 获取指定用户最新的10条系统日志
     *
     * @param userId 用户ID
     * @return 系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getTop10LogsByUserId(Integer userId) {
        Page<SystemLog> page = systemLogRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
        return SystemLogListResponse.of(page.getContent().stream()
                .map(SystemLogResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * 分页查询所有系统日志
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getLogs(Integer page, Integer size) {
        Pageable pageable = createPageable(page, size);
        Page<SystemLog> logPage = systemLogRepository.findAll(pageable);
        return toResponse(logPage);
    }

    /**
     * 按模块分页查询系统日志
     *
     * @param module 模块名称
     * @param page 页码
     * @param size 每页大小
     * @return 分页系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getLogsByModule(String module, Integer page, Integer size) {
        Pageable pageable = createPageable(page, size);
        Page<SystemLog> logPage = systemLogRepository.findByModule(module, pageable);
        return toResponse(logPage);
    }

    /**
     * 按用户ID分页查询系统日志
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getLogsByUserId(Integer userId, Integer page, Integer size) {
        Pageable pageable = createPageable(page, size);
        Page<SystemLog> logPage = systemLogRepository.findByUserId(userId, pageable);
        return toResponse(logPage);
    }

    /**
     * 按模块和用户ID分页查询系统日志
     *
     * @param module 模块名称
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页系统日志列表响应
     */
    @Transactional(readOnly = true)
    public SystemLogListResponse getLogsByModuleAndUserId(String module, Integer userId, Integer page, Integer size) {
        Pageable pageable = createPageable(page, size);
        Page<SystemLog> logPage = systemLogRepository.findByModuleAndUserId(module, userId, pageable);
        return toResponse(logPage);
    }

    /**
     * 根据手机号获取用户实体
     *
     * @param phone 手机号
     * @return 用户实体
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional(readOnly = true)
    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    /**
     * 分页查询所有系统日志（返回原始Page对象）
     *
     * @param pageable 分页参数
     * @return 系统日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<SystemLog> findAll(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }

    /**
     * 按用户ID分页查询系统日志（返回原始Page对象）
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 系统日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<SystemLog> findByUserId(Integer userId, Pageable pageable) {
        return systemLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 按模块分页查询系统日志（返回原始Page对象）
     *
     * @param module 模块名称
     * @param pageable 分页参数
     * @return 系统日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<SystemLog> findByModule(String module, Pageable pageable) {
        return systemLogRepository.findByModule(module, pageable);
    }

    /**
     * 按模块和用户ID分页查询系统日志（返回原始Page对象）
     *
     * @param module 模块名称
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 系统日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<SystemLog> findByModuleAndUserId(String module, Integer userId, Pageable pageable) {
        return systemLogRepository.findByModuleAndUserId(module, userId, pageable);
    }

    /**
     * 获取所有不重复的模块名称
     *
     * @return 模块名称列表
     */
    @Transactional(readOnly = true)
    public List<String> findDistinctModules() {
        return systemLogRepository.findDistinctModules();
    }

    /**
     * 统计所有系统日志数量
     *
     * @return 日志总数
     */
    @Transactional(readOnly = true)
    public long countAllLogs() {
        return systemLogRepository.count();
    }

    /**
     * 统计今日系统日志数量
     *
     * @return 今日日志数
     */
    @Transactional(readOnly = true)
    public long countTodayLogs() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        return systemLogRepository.countByCreatedAtAfter(today);
    }

    /**
     * 统计失败的系统日志数量
     *
     * @return 失败日志数
     */
    @Transactional(readOnly = true)
    public long countFailedLogs() {
        return systemLogRepository.countBySuccess(false);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建带分页校验和默认排序的Pageable
     */
    private Pageable createPageable(Integer page, Integer size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        return PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * 将Page&lt;SystemLog&gt;转换为SystemLogListResponse
     */
    private SystemLogListResponse toResponse(Page<SystemLog> page) {
        List<SystemLogResponse> list = page.getContent().stream()
                .map(SystemLogResponse::fromEntity)
                .collect(Collectors.toList());
        return new SystemLogListResponse(page.getTotalElements(), page.getTotalPages(), list);
    }
}