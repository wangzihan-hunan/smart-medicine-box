package com.example.medicinebox.service;

import com.example.medicinebox.config.AuthProperties;
import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.request.UserRegisterRequest;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.RegisterResponse;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.AccountLockedException;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.security.JwtUtil;
import com.example.medicinebox.util.UserInfoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证授权服务类
 * 核心功能：用户注册、登录认证、个人信息管理、密码修改
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final MetricsService metricsService;
    private final AuthProperties authProperties;

    // 自引用代理，用于在同类方法调用时仍能走事务切面（REQUIRES_NEW）
    private final AuthService self;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       MetricsService metricsService, AuthProperties authProperties,
                       @Lazy AuthService self) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.metricsService = metricsService;
        this.authProperties = authProperties;
        this.self = self;
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
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> {
                    log.warn("用户不存在: {}", request.getPhone());
                    metricsService.recordUserLogin(false);
                    return new BadCredentialsException("手机号或密码错误！");
                });

        checkAccountLocked(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
            );

            self.resetLoginAttempts(user);

            String token = jwtUtil.generateToken(user.getPhone(), user.getRole().name());

            log.debug("用户登录成功, 用户ID: {}", user.getId());
            metricsService.recordUserLogin(true);
            return new LoginResponse(token, "bearer", user.getMustChangePassword() != null && user.getMustChangePassword());
        } catch (BadCredentialsException e) {
            log.warn("用户登录失败, 手机号: {}, 尝试次数: {}", request.getPhone(), user.getLoginAttempts() + 1);
            self.incrementLoginAttempts(user);
            metricsService.recordUserLogin(false);
            throw e;
        }
    }

    // 检查账号是否被锁定，若锁定时间已过则自动解锁
    private void checkAccountLocked(User user) {
        if (user.isLocked()) {
            if (user.shouldAutoUnlock(authProperties.getLockDurationMinutes())) {
                log.debug("账号自动解锁, 用户ID: {}", user.getId());
                user.unlock();
                userRepository.save(user);
            } else {
                long remainingMinutes = java.time.Duration.between(
                        LocalDateTime.now(),
                        user.getLockTime().plusMinutes(authProperties.getLockDurationMinutes())
                ).toMinutes();
                log.warn("账号被锁定, 用户ID: {}, 剩余锁定时间: {}分钟", user.getId(), remainingMinutes);
                throw new AccountLockedException("账号已被锁定，请" + remainingMinutes + "分钟后再试");
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
        user.incrementLoginAttempts(authProperties.getMaxLoginAttempts());
        userRepository.save(user);
        log.debug("登录尝试次数增加, 用户ID: {}, 当前次数: {}", user.getId(), user.getLoginAttempts());
    }

    /**
     * 重置登录尝试次数（新事务，确保成功立即持久化）
     * 登录成功后调用，重置尝试次数并更新最后登录时间
     *
     * @param user 用户实体
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetLoginAttempts(User user) {
        user.resetLoginAttempts();
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
        return UserInfoConverter.fromUser(user);
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

        if (name != null && !name.isEmpty()) {
            log.debug("更新用户信息, 用户ID: {}, 新姓名: {}", user.getId(), name);
            user.setName(name);
        }

        user = userRepository.save(user);

        return new UserInfoResponse(user.getId(), user.getPhone(), user.getName(),
                user.getRole().name(), user.getPhoneVerified());
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

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("密码修改失败, 旧密码不正确, 用户ID: {}", user.getId());
            throw new BadRequestException("当前密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.debug("密码修改成功, 用户ID: {}", user.getId());
    }
}