package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.request.UserRegisterRequest;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.RegisterResponse;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.dto.response.UserListResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.AccountLockedException;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.LocalDateTime;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证授权服务类
 * 核心功能：用户注册、登录认证、账号管理（锁定/解锁/封禁/冻结）、用户信息管理、密码修改、管理员用户管理等
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final MetricsService metricsService;

    // 自引用代理，用于在同类方法调用时仍能走事务切面（REQUIRES_NEW）
    @Autowired
    @Lazy
    private AuthService self;

    /**
     * 构造函数，注入依赖
     *
     * @param userRepository 用户数据访问层
     * @param passwordEncoder 密码编码器
     * @param jwtUtil JWT工具类
     * @param authenticationManager 认证管理器
     * @param metricsService 指标统计服务
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       MetricsService metricsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.metricsService = metricsService;
    }

    /**
     * 用户注册
     * 校验手机号是否已注册，密码加密后保存用户信息
     *
     * @param request 注册请求（手机号、密码、姓名）
     * @return 注册响应（用户ID）
     * @throws BadRequestException 手机号已注册时抛出
     */
    @Transactional
    public RegisterResponse register(UserRegisterRequest request) {
        log.debug("用户注册, 手机号: {}", request.getPhone());
        if (userRepository.existsByPhone(request.getPhone())) {
            log.warn("手机号已注册: {}", request.getPhone());
            metricsService.recordBusinessException("register_duplicate");
            throw new BadRequestException("该手机号已注册，请直接登录！");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        user = userRepository.save(user);

        log.debug("用户注册成功, 用户ID: {}", user.getId());
        metricsService.recordUserRegister();
        return new RegisterResponse(user.getId());
    }

    /**
     * 用户登录
     * 验证用户账号状态，通过认证管理器校验密码，登录成功生成JWT令牌，失败则累计尝试次数
     *
     * @param request 登录请求（手机号、密码）
     * @return 登录响应（JWT令牌、令牌类型）
     * @throws BadCredentialsException 手机号或密码错误时抛出
     * @throws AccountLockedException 账号被锁定时抛出
     */
    public LoginResponse login(UserLoginRequest request) {
        log.debug("用户登录, 手机号: {}", request.getPhone());
        // 根据手机号查询用户，不存在则抛出认证异常
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> {
                    log.warn("用户不存在: {}", request.getPhone());
                    metricsService.recordUserLogin(false);
                    return new BadCredentialsException("手机号或密码错误！");
                });

        // 检查账号是否被锁定
        checkAccountLocked(user);

        try {
            // 使用Spring Security认证管理器进行密码校验
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
            );

            // 登录成功，重置登录尝试次数（使用自引用确保新事务生效）
            self.resetLoginAttempts(user);

            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getPhone());

            log.debug("用户登录成功, 用户ID: {}", user.getId());
            metricsService.recordUserLogin(true);
            return new LoginResponse(token, "bearer", user.getMustChangePassword() != null && user.getMustChangePassword());
        } catch (BadCredentialsException e) {
            // 登录失败，增加登录尝试次数（使用自引用确保新事务生效）
            log.warn("用户登录失败, 手机号: {}, 尝试次数: {}", request.getPhone(), user.getLoginAttempts() + 1);
            self.incrementLoginAttempts(user);
            metricsService.recordUserLogin(false);
            throw e;
        }
    }

    // 检查账号是否被锁定，若锁定时间已过则自动解锁
    private void checkAccountLocked(User user) {
        if ("LOCKED".equals(user.getStatus()) && user.getLockTime() != null) {
            LocalDateTime lockTime = user.getLockTime();
            LocalDateTime unlockTime = lockTime.plusMinutes(30);
            
            if (LocalDateTime.now().isBefore(unlockTime)) {
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), unlockTime).toMinutes();
                log.warn("账号被锁定, 用户ID: {}, 剩余锁定时间: {}分钟", user.getId(), remainingMinutes);
                throw new AccountLockedException("账号已被锁定，请" + remainingMinutes + "分钟后再试");
            } else {
                log.debug("账号自动解锁, 用户ID: {}", user.getId());
                user.setStatus("ACTIVE");
                user.setLockTime(null);
                user.setLoginAttempts(0);
                userRepository.save(user);
            }
        }
    }

    /**
     * 增加登录尝试次数（新事务，确保失败也能持久化）
     * 当尝试次数达到5次时，自动锁定账号30分钟
     *
     * @param user 用户实体
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementLoginAttempts(User user) {
        int newAttempts = user.getLoginAttempts() + 1;
        user.setLoginAttempts(newAttempts);

        // 尝试次数达到5次，锁定账号
        if (newAttempts >= 5) {
            user.setStatus("LOCKED");
            user.setLockTime(LocalDateTime.now());
            log.warn("账号被锁定, 用户ID: {}", user.getId());
        }

        userRepository.save(user);
        log.debug("登录尝试次数增加, 用户ID: {}, 当前次数: {}", user.getId(), newAttempts);
    }

    /**
     * 重置登录尝试次数（新事务，确保成功立即持久化）
     * 登录成功后调用，重置尝试次数并更新最后登录时间
     *
     * @param user 用户实体
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetLoginAttempts(User user) {
        user.setLoginAttempts(0);
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);
        log.debug("登录尝试次数重置, 用户ID: {}", user.getId());
    }

    /**
     * 获取当前登录用户的信息
     * 从Security上下文中获取当前用户，返回用户详情
     *
     * @return 用户信息响应
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    public UserInfoResponse getCurrentUserInfo() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        log.debug("获取用户信息, 用户ID: {}", user.getId());
        return createUserInfoResponse(user);
    }

    /**
     * 更新当前用户信息
     * 目前支持更新用户姓名
     *
     * @param name 新的用户姓名
     * @return 更新后的用户信息响应
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public UserInfoResponse updateUserInfo(String name) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 姓名非空则更新
        if (name != null && !name.isEmpty()) {
            log.debug("更新用户信息, 用户ID: {}, 新姓名: {}", user.getId(), name);
            user.setName(name);
        }

        user = userRepository.save(user);

        return new UserInfoResponse(user.getId(), user.getPhone(), user.getName(), 
                user.getRole(), user.getPhoneVerified());
    }

    /**
     * 修改当前用户密码
     * 校验旧密码正确后，更新为新密码（加密存储）
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws ResourceNotFoundException 用户不存在时抛出
     * @throws BadRequestException 旧密码不正确时抛出
     */
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 校验旧密码是否匹配
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("密码修改失败, 旧密码不正确, 用户ID: {}", user.getId());
            throw new BadRequestException("当前密码不正确");
        }

        // 加密新密码并保存，清除强制改密标识
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.debug("密码修改成功, 用户ID: {}", user.getId());
    }

    /**
     * 获取所有用户列表（不分页）
     *
     * @return 用户列表响应
     */
    public UserListResponse getAllUsers() {
        log.debug("获取所有用户列表");
        List<User> users = userRepository.findAll();
        
        List<UserInfoResponse> responseList = users.stream()
                .map(user -> new UserInfoResponse(user.getId(), user.getPhone(), user.getName(), 
                        user.getRole(), user.getPhoneVerified()))
                .collect(Collectors.toList());

        log.debug("获取用户列表成功, 数量: {}", users.size());
        return new UserListResponse((int) users.size(), responseList);
    }

    /**
     * 分页获取所有用户列表
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页用户列表响应
     */
    public UserListResponse getAllUsers(int page, int size) {
        log.debug("获取用户列表, page: {}, size: {}", page, size);
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, size));
        return buildUserListResponse(userPage, page, size);
    }

    /**
     * 搜索用户（按手机号或姓名模糊搜索）
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 分页用户列表响应
     */
    public UserListResponse searchUsers(String keyword, int page, int size) {
        log.debug("搜索用户, keyword: {}, page: {}, size: {}", keyword, page, size);
        Page<User> userPage = userRepository.findByPhoneContainingOrNameContaining(
                keyword, keyword, PageRequest.of(page, size));
        return buildUserListResponse(userPage, page, size);
    }

    // 构建分页用户列表响应对象
    private UserListResponse buildUserListResponse(Page<User> userPage, int page, int size) {
        List<UserInfoResponse> responseList = userPage.getContent().stream()
                .map(user -> createUserInfoResponse(user))
                .collect(Collectors.toList());

        UserListResponse response = new UserListResponse();
        response.setTotalElements((int) userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setList(responseList);
        return response;
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息响应，用户不存在返回null
     */
    public UserInfoResponse getUserById(Integer id) {
        log.debug("获取用户详情, ID: {}", id);
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        return createUserInfoResponse(user);
    }

    /**
     * 更新用户状态
     * 若激活状态则同时清除锁定时间和登录尝试次数
     *
     * @param id 用户ID
     * @param status 新状态（ACTIVE/LOCKED/BLOCKED/SUSPENDED）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserStatus(Integer id, String status) {
        log.debug("更新用户状态, ID: {}, status: {}", id, status);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setStatus(status);
        // 激活状态时重置锁定相关字段
        if ("ACTIVE".equals(status)) {
            user.setLockTime(null);
            user.setLoginAttempts(0);
        }
        userRepository.save(user);
    }

    /**
     * 封禁用户
     * 将用户状态设为BLOCKED并记录封禁时间
     *
     * @param id 用户ID
     * @param reason 封禁原因
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void blockUser(Integer id, String reason) {
        log.debug("封禁用户, ID: {}, reason: {}", id, reason);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setStatus("BLOCKED");
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 冻结用户
     * 将用户状态设为SUSPENDED并记录冻结时间
     *
     * @param id 用户ID
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void freezeUser(Integer id) {
        log.debug("冻结用户, ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setStatus("SUSPENDED");
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 解封用户
     * 将用户状态设为ACTIVE，清除锁定时间和登录尝试次数
     *
     * @param id 用户ID
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void unblockUser(Integer id) {
        log.debug("解封用户, ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setStatus("ACTIVE");
        user.setLockTime(null);
        user.setLoginAttempts(0);
        userRepository.save(user);
    }

    /**
     * 更新用户角色
     *
     * @param id 用户ID
     * @param role 新角色（USER/ADMIN等）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserRole(Integer id, String role) {
        log.debug("更新用户角色, ID: {}, role: {}", id, role);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setRole(role);
        userRepository.save(user);
    }

    /**
     * 更新用户姓名
     *
     * @param id 用户ID
     * @param name 新姓名
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserName(Integer id, String name) {
        log.debug("更新用户姓名, ID: {}, name: {}", id, name);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setName(name);
        userRepository.save(user);
    }

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @param newPassword 新密码（明文，方法内加密）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void resetUserPassword(Integer id, String newPassword) {
        log.debug("重置用户密码, ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 更新用户每月AI识别限额
     *
     * @param id 用户ID
     * @param limit 新的AI识别限额
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateMonthAiLimit(Integer id, Integer limit) {
        log.debug("更新用户AI限额, ID: {}, limit: {}", id, limit);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        user.setMonthAiLimit(limit);
        userRepository.save(user);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void deleteUser(Integer id) {
        log.debug("删除用户, ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    /**
     * 批量删除用户
     * 仅删除非管理员角色的用户，返回实际删除数量
     *
     * @param ids 用户ID列表
     * @return 实际删除的用户数量
     */
    @Transactional
    public int batchDeleteUsers(List<Integer> ids) {
        log.debug("批量删除用户, IDs: {}", ids);
        int deletedCount = 0;
        for (Integer id : ids) {
            if (userRepository.existsById(id)) {
                User user = userRepository.findById(id).orElse(null);
                // 保护管理员账号不被删除
                if (user != null && !"ADMIN".equals(user.getRole())) {
                    userRepository.deleteById(id);
                    deletedCount++;
                }
            }
        }
        log.info("批量删除用户完成, 删除数量: {}", deletedCount);
        return deletedCount;
    }

    /**
     * 批量更新用户状态
     * 仅更新非管理员角色的用户，返回实际更新数量
     *
     * @param ids 用户ID列表
     * @param status 新状态
     * @return 实际更新的用户数量
     */
    @Transactional
    public int batchUpdateUserStatus(List<Integer> ids, String status) {
        log.debug("批量更新用户状态, IDs: {}, status: {}", ids, status);
        int updatedCount = 0;
        for (Integer id : ids) {
            User user = userRepository.findById(id).orElse(null);
            // 保护管理员账号不被更新
            if (user != null && !"ADMIN".equals(user.getRole())) {
                user.setStatus(status);
                // 激活状态时重置锁定相关字段
                if ("ACTIVE".equals(status)) {
                    user.setLockTime(null);
                    user.setLoginAttempts(0);
                }
                userRepository.save(user);
                updatedCount++;
            }
        }
        log.info("批量更新用户状态完成, 更新数量: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 批量更新用户角色
     * 仅更新非管理员角色的用户，返回实际更新数量
     *
     * @param ids 用户ID列表
     * @param role 新角色
     * @return 实际更新的用户数量
     */
    @Transactional
    public int batchUpdateUserRole(List<Integer> ids, String role) {
        log.debug("批量更新用户角色, IDs: {}, role: {}", ids, role);
        int updatedCount = 0;
        for (Integer id : ids) {
            User user = userRepository.findById(id).orElse(null);
            // 保护管理员账号不被修改角色
            if (user != null && !"ADMIN".equals(user.getRole())) {
                user.setRole(role);
                userRepository.save(user);
                updatedCount++;
            }
        }
        log.info("批量更新用户角色完成, 更新数量: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 管理员创建用户
     * 支持设置角色、套餐、AI限额等，根据套餐类型设置过期时间
     *
     * @param request 管理员创建用户请求
     * @return 创建的用户实体
     * @throws BadRequestException 手机号已注册时抛出
     */
    @Transactional
    public User createUserByAdmin(com.example.medicinebox.dto.request.AdminCreateUserRequest request) {
        log.debug("管理员创建用户, 手机号: {}", maskPhone(request.getPhone()));

        // 检查手机号是否已注册
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("手机号已注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus("ACTIVE");
        user.setPhoneVerified(false);
        user.setPlan(request.getPlan() != null ? request.getPlan() : "FREE");
        user.setMonthAiLimit(request.getMonthAiLimit() != null ? request.getMonthAiLimit() : 10);
        user.setMonthAiCount(0);
        user.setLoginAttempts(0);

        // 根据套餐类型设置过期时间
        if ("PROFESSIONAL".equals(user.getPlan())) {
            user.setPlanExpireAt(LocalDateTime.now().plusYears(1));
        } else if ("BASIC".equals(user.getPlan())) {
            user.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        }

        user = userRepository.save(user);
        log.info("管理员创建用户成功, ID: {}, 手机号: {}", user.getId(), maskPhone(user.getPhone()));
        return user;
    }

    // 根据用户实体构建用户信息响应对象（含手机号脱敏）
    private UserInfoResponse createUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setPhone(maskPhone(user.getPhone()));
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setPhoneVerified(user.getPhoneVerified());
        response.setStatus(user.getStatus());
        response.setPlan(user.getPlan());
        response.setPlanExpireAt(user.getPlanExpireAt());
        response.setMonthAiCount(user.getMonthAiCount());
        response.setMonthAiLimit(user.getMonthAiLimit());
        response.setLoginAttempts(user.getLoginAttempts());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setLockTime(user.getLockTime());
        response.setLastIp(user.getLastIp());
        return response;
    }

    /**
     * 根据手机号获取用户实体（返回实体对象，非响应对象）
     * 供其他服务内部调用使用
     *
     * @param phone 手机号
     * @return 用户实体，不存在返回null
     */
    public com.example.medicinebox.entity.User getUserEntityByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    // 手机号脱敏：保留前3位和后4位，中间用****替换
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 验证当前管理员密码（用于高危操作二次确认）
     *
     * @param adminPhone 管理员手机号（从 SecurityContext 获取）
     * @param password 待验证的密码
     * @return 密码是否正确
     */
    public boolean verifyAdminPassword(String adminPhone, String password) {
        User admin = userRepository.findByPhone(adminPhone).orElse(null);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return false;
        }
        return passwordEncoder.matches(password, admin.getPassword());
    }

    // 验证码存储（内存缓存，生产环境建议使用Redis）
    // key: 手机号, value: {captcha, expireTime}
    private static final Map<String, Map<String, Object>> captchaCache = new ConcurrentHashMap<>();
    // 验证码过期时间：5分钟
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    // 验证码发送间隔：60秒
    private static final long CAPTCHA_SEND_INTERVAL_SECONDS = 60;

    /**
     * 发送验证码（用于找回密码）
     * 生成4位随机验证码，存储到内存缓存中，模拟发送短信
     * 生产环境应集成真实短信服务（如阿里云短信、腾讯云短信等）
     *
     * @param phone 接收验证码的手机号
     * @throws BadRequestException 用户不存在或发送过于频繁时抛出
     */
    public void sendCaptcha(String phone) {
        log.debug("发送验证码, 手机号: {}", maskPhone(phone));

        // 检查用户是否存在
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            log.warn("用户不存在, 无法发送验证码: {}", maskPhone(phone));
            throw new BadRequestException("该手机号未注册");
        }

        // 检查发送频率
        Map<String, Object> cached = captchaCache.get(phone);
        if (cached != null) {
            long lastSendTime = (Long) cached.get("sendTime");
            if (System.currentTimeMillis() - lastSendTime < CAPTCHA_SEND_INTERVAL_SECONDS * 1000) {
                log.warn("验证码发送过于频繁: {}", maskPhone(phone));
                throw new BadRequestException("验证码发送过于频繁，请稍后再试");
            }
        }

        // 生成4位随机验证码
        String captcha = generateCaptcha();

        // 存储验证码（模拟）
        Map<String, Object> captchaData = new ConcurrentHashMap<>();
        captchaData.put("captcha", captcha);
        captchaData.put("expireTime", System.currentTimeMillis() + CAPTCHA_EXPIRE_MINUTES * 60 * 1000);
        captchaData.put("sendTime", System.currentTimeMillis());
        captchaCache.put(phone, captchaData);

        // 模拟发送短信（生产环境替换为真实短信服务）
        log.info("验证码已发送, 手机号: {}, 验证码: {} (模拟)", maskPhone(phone), captcha);
        // 实际项目中应调用短信API：smsService.send(phone, "您的验证码是：" + captcha + "，5分钟内有效。");
    }

    /**
     * 生成4位随机数字验证码
     *
     * @return 4位数字验证码
     */
    private String generateCaptcha() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    /**
     * 重置密码（通过验证码）
     * 验证验证码有效后，更新用户密码
     *
     * @param phone 手机号
     * @param captcha 验证码
     * @param newPassword 新密码（明文）
     * @throws BadRequestException 验证码无效、过期或用户不存在时抛出
     */
    @Transactional
    public void resetPassword(String phone, String captcha, String newPassword) {
        log.debug("重置密码, 手机号: {}", maskPhone(phone));

        // 检查用户是否存在
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException("该手机号未注册"));

        // 验证验证码
        Map<String, Object> cached = captchaCache.get(phone);
        if (cached == null) {
            log.warn("验证码不存在, 手机号: {}", maskPhone(phone));
            throw new BadRequestException("验证码不存在，请重新获取");
        }

        String cachedCaptcha = (String) cached.get("captcha");
        long expireTime = (Long) cached.get("expireTime");

        // 检查验证码是否过期
        if (System.currentTimeMillis() > expireTime) {
            log.warn("验证码已过期, 手机号: {}", maskPhone(phone));
            captchaCache.remove(phone);
            throw new BadRequestException("验证码已过期，请重新获取");
        }

        // 检查验证码是否正确
        if (!captcha.equals(cachedCaptcha)) {
            log.warn("验证码错误, 手机号: {}", maskPhone(phone));
            throw new BadRequestException("验证码错误，请重新输入");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginAttempts(0); // 重置登录尝试次数
        userRepository.save(user);

        // 清除验证码缓存
        captchaCache.remove(phone);

        log.info("密码重置成功, 用户ID: {}", user.getId());
    }
}
