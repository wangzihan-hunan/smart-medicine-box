package com.example.medicinebox.service;

import com.example.medicinebox.config.AuthProperties;
import com.example.medicinebox.constant.UserPlan;
import com.example.medicinebox.constant.UserRole;
import com.example.medicinebox.constant.UserStatus;
import com.example.medicinebox.dto.request.AdminCreateUserRequest;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.dto.response.UserListResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.PhoneUtils;
import com.example.medicinebox.util.UserInfoConverter;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务类
 * 负责管理员对用户的管理操作：查询、状态变更、角色管理、创建、删除等
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    /**
     * 获取所有用户列表（不分页）
     *
     * @return 用户列表响应
     * @deprecated 请使用 {@link #getAllUsers(int, int)} 替代
     */
    @Deprecated
    public UserListResponse getAllUsers() {
        log.debug("获取所有用户列表");
        List<User> users = userRepository.findAll();

        List<UserInfoResponse> responseList = users.stream()
                .map(user -> new UserInfoResponse(user.getId(), user.getPhone(), user.getName(),
                        user.getRole().name(), user.getPhoneVerified()))
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
    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(int page, int size) {
        log.debug("获取用户列表, page: {}, size: {}", page, size);
        int validatedSize = PaginationUtil.validateSize(size);
        int validatedPage = PaginationUtil.validatePage(page);
        Page<User> userPage = userRepository.findAll(PageRequest.of(validatedPage, validatedSize));
        return buildUserListResponse(userPage, validatedPage, validatedSize);
    }

    /**
     * 搜索用户（按手机号或姓名模糊搜索）
     *
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页大小
     * @return 分页用户列表响应
     */
    @Transactional(readOnly = true)
    public UserListResponse searchUsers(String keyword, int page, int size) {
        log.debug("搜索用户, keyword: {}, page: {}, size: {}", keyword, page, size);
        int validatedSize = PaginationUtil.validateSize(size);
        int validatedPage = PaginationUtil.validatePage(page);
        Page<User> userPage = userRepository.findByPhoneContainingOrNameContaining(
                keyword, keyword, PageRequest.of(validatedPage, validatedSize));
        return buildUserListResponse(userPage, validatedPage, validatedSize);
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息响应，用户不存在返回null
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserById(Integer id) {
        log.debug("获取用户详情, ID: {}", id);
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        return UserInfoConverter.fromUser(user);
    }

    /**
     * 更新用户状态
     * 若激活状态则同时清除锁定时间和登录尝试次数
     *
     * @param id     用户ID
     * @param status 新状态（ACTIVE/LOCKED/BLOCKED/SUSPENDED）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserStatus(Integer id, String status) {
        log.debug("更新用户状态, ID: {}, status: {}", id, status);
        User user = getUserOrThrow(id);
        user.setStatus(UserStatus.valueOf(status));
        if (UserStatus.ACTIVE == user.getStatus()) {
            user.unlock();
        }
        userRepository.save(user);
    }

    /**
     * 封禁用户
     *
     * @param id     用户ID
     * @param reason 封禁原因
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void blockUser(Integer id, String reason) {
        log.debug("封禁用户, ID: {}, reason: {}", id, reason);
        User user = getUserOrThrow(id);
        user.block();
        userRepository.save(user);
    }

    /**
     * 冻结用户
     *
     * @param id 用户ID
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void freezeUser(Integer id) {
        log.debug("冻结用户, ID: {}", id);
        User user = getUserOrThrow(id);
        user.suspend();
        userRepository.save(user);
    }

    /**
     * 解封用户
     *
     * @param id 用户ID
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void unblockUser(Integer id) {
        log.debug("解封用户, ID: {}", id);
        User user = getUserOrThrow(id);
        user.unlock();
        userRepository.save(user);
    }

    /**
     * 更新用户角色
     *
     * @param id   用户ID
     * @param role 新角色（USER/ADMIN等）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserRole(Integer id, String role) {
        log.debug("更新用户角色, ID: {}, role: {}", id, role);
        User user = getUserOrThrow(id);
        user.setRole(UserRole.valueOf(role));
        userRepository.save(user);
    }

    /**
     * 更新用户姓名
     *
     * @param id   用户ID
     * @param name 新姓名
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateUserName(Integer id, String name) {
        log.debug("更新用户姓名, ID: {}, name: {}", id, name);
        User user = getUserOrThrow(id);
        user.setName(name);
        userRepository.save(user);
    }

    /**
     * 重置用户密码
     *
     * @param id          用户ID
     * @param newPassword 新密码（明文，方法内加密）
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void resetUserPassword(Integer id, String newPassword) {
        log.debug("重置用户密码, ID: {}", id);
        User user = getUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 更新用户每月AI识别限额
     *
     * @param id    用户ID
     * @param limit 新的AI识别限额
     * @throws ResourceNotFoundException 用户不存在时抛出
     */
    @Transactional
    public void updateMonthAiLimit(Integer id, Integer limit) {
        log.debug("更新用户AI限额, ID: {}, limit: {}", id, limit);
        User user = getUserOrThrow(id);
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
     * 管理员创建用户
     *
     * @param request 管理员创建用户请求
     * @return 创建的用户实体
     * @throws BadRequestException 手机号已注册时抛出
     */
    @Transactional
    public User createUserByAdmin(AdminCreateUserRequest request) {
        log.debug("管理员创建用户, 手机号: {}", PhoneUtils.maskPhone(request.getPhone()));

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("手机号已注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? UserRole.valueOf(request.getRole()) : UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPhoneVerified(false);
        user.setPlan(request.getPlan() != null ? UserPlan.valueOf(request.getPlan()) : UserPlan.FREE);
        user.setMonthAiLimit(request.getMonthAiLimit() != null ? request.getMonthAiLimit() : authProperties.getDefaultAiLimit());
        user.setMonthAiCount(0);
        user.setLoginAttempts(0);

        if (UserPlan.PROFESSIONAL == user.getPlan()) {
            user.setPlanExpireAt(LocalDateTime.now().plusYears(1));
        } else if (UserPlan.BASIC == user.getPlan()) {
            user.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        }

        user = userRepository.save(user);
        log.info("管理员创建用户成功, ID: {}, 手机号: {}", user.getId(), PhoneUtils.maskPhone(user.getPhone()));
        return user;
    }

    /**
     * 根据手机号获取用户实体
     *
     * @param phone 手机号
     * @return 用户实体，不存在返回null
     */
    @Transactional(readOnly = true)
    public User getUserEntityByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    /**
     * 验证管理员密码（用于高危操作二次确认）
     *
     * @param adminPhone 管理员手机号
     * @param password   待验证的密码
     * @return 密码是否正确
     */
    @Transactional(readOnly = true)
    public boolean verifyAdminPassword(String adminPhone, String password) {
        User admin = userRepository.findByPhone(adminPhone).orElse(null);
        if (admin == null || !admin.isAdmin()) {
            return false;
        }
        return passwordEncoder.matches(password, admin.getPassword());
    }

    // 构建分页用户列表响应对象
    private UserListResponse buildUserListResponse(Page<User> userPage, int page, int size) {
        List<UserInfoResponse> responseList = userPage.getContent().stream()
                .map(UserInfoConverter::fromUser)
                .collect(Collectors.toList());

        UserListResponse response = new UserListResponse();
        response.setTotalElements((int) userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setList(responseList);
        return response;
    }

    // 根据ID获取用户，不存在时抛出异常
    private User getUserOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}