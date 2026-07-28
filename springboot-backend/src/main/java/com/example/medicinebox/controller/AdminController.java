package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.AdminCreateUserRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.dto.response.UserListResponse;
import com.example.medicinebox.entity.SystemLog;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.service.AuthService;
import com.example.medicinebox.service.PlanService;
import com.example.medicinebox.service.SystemLogService;
import com.example.medicinebox.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台控制器
 * 提供管理员登录、仪表盘统计、用户管理、系统日志查询等管理功能
 * 大部分接口需要管理员角色权限
 * 路由前缀：/api/v1/admin
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AuthService authService;
    private final PlanService planService;
    private final SystemLogService systemLogService;

    public AdminController(AuthService authService, PlanService planService, SystemLogService systemLogService) {
        this.authService = authService;
        this.planService = planService;
        this.systemLogService = systemLogService;
    }

    /**
     * 管理员登录接口
     * HTTP方法：POST
     * 权限要求：公开（无需登录，但账号需为管理员角色）
     * 验证用户身份并检查是否具有管理员权限
     * 
     * @param request 登录请求体，包含手机号和密码
     * @return 登录成功响应，包含访问令牌等信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponse> adminLogin(@RequestBody com.example.medicinebox.dto.request.UserLoginRequest request) {
        log.info("管理员登录请求, 手机号: {}", request.getPhone());
        
        LoginResponse response = authService.login(request);
        
        com.example.medicinebox.entity.User user = authService.getUserEntityByPhone(request.getPhone());
        if (user != null && "ADMIN".equals(user.getRole())) {
            log.info("管理员登录成功, 手机号: {}", request.getPhone());
            return BaseResponse.success("登录成功！", response);
        } else {
            log.warn("非管理员账号尝试登录管理后台, 手机号: {}", request.getPhone());
            return BaseResponse.error(403, "无管理员权限");
        }
    }

    /**
     * 获取管理后台仪表盘数据接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 返回用户统计数据，包括总用户数、活跃用户数、管理员数、付费用户数等
     * 
     * @return 仪表盘统计数据Map
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Map<String, Object>> getDashboard() {
        log.info("获取管理后台仪表盘数据");
        
        Map<String, Object> dashboard = new HashMap<>();
        
        UserListResponse userList = authService.getAllUsers();
        dashboard.put("totalUsers", userList.getTotalElements());
        
        long activeUsers = userList.getList().stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .count();
        dashboard.put("activeUsers", activeUsers);
        
        long adminUsers = userList.getList().stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .count();
        dashboard.put("adminUsers", adminUsers);
        
        long paidUsers = userList.getList().stream()
                .filter(u -> !"FREE".equals(u.getPlan()))
                .count();
        dashboard.put("paidUsers", paidUsers);
        
        return BaseResponse.success("获取成功", dashboard);
    }

    /**
     * 分页查询用户列表接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 支持按关键词搜索用户
     * 
     * @param keyword 搜索关键词（可选）
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 用户列表响应，包含总数和用户信息列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<UserListResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("获取用户列表, keyword: {}, page: {}, size: {}", keyword, validatedPage, validatedSize);
        UserListResponse response;
        if (keyword != null && !keyword.isEmpty()) {
            response = authService.searchUsers(keyword, validatedPage, validatedSize);
        } else {
            response = authService.getAllUsers(validatedPage, validatedSize);
        }
        return BaseResponse.success("获取成功", response);
    }

    /**
     * 获取用户详情接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @return 用户详细信息
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<UserInfoResponse> getUser(@PathVariable Integer id) {
        log.info("获取用户详情, ID: {}", id);
        UserInfoResponse response = authService.getUserById(id);
        if (response == null) {
            return BaseResponse.error(404, "用户不存在");
        }
        return BaseResponse.success("获取成功", response);
    }

    /**
     * 更新用户状态接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param status 用户状态（ACTIVE/INACTIVE等）
     * @return 操作结果，无返回数据
     */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> updateUserStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        log.info("更新用户状态, ID: {}, status: {}", id, status);
        authService.updateUserStatus(id, status);
        return BaseResponse.success("更新成功");
    }

    /**
     * 更新用户姓名接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param name 新的用户姓名
     * @return 操作结果
     */
    @PutMapping("/users/{id}/name")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<String> updateUserName(
            @PathVariable Integer id,
            @RequestParam String name) {
        log.info("更新用户姓名, ID: {}, name: {}", id, name);
        authService.updateUserName(id, name);
        return BaseResponse.success("姓名更新成功", null);
    }

    /**
     * 更新用户角色接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param role 新的用户角色（USER/ADMIN等）
     * @return 操作结果
     */
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<String> updateUserRole(
            @PathVariable Integer id,
            @RequestParam String role) {
        log.info("更新用户角色, ID: {}, role: {}", id, role);
        authService.updateUserRole(id, role);
        return BaseResponse.success("角色更新成功", null);
    }

    /**
     * 重置用户密码接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param newPassword 新密码，默认123456
     * @return 操作结果
     */
    @PutMapping("/users/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<String> resetUserPassword(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "123456") String newPassword) {
        log.info("重置用户密码, ID: {}", id);
        authService.resetUserPassword(id, newPassword);
        return BaseResponse.success("密码重置成功", null);
    }

    /**
     * 更新用户套餐接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param plan 套餐类型（FREE/BASIC/PROFESSIONAL）
     * @param months 套餐时长，默认1个月
     * @return 操作结果，无返回数据
     */
    @PutMapping("/users/{id}/plan")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> updateUserPlan(
            @PathVariable Integer id,
            @RequestParam String plan,
            @RequestParam(defaultValue = "1") int months) {
        log.info("更新用户套餐, ID: {}, plan: {}, months: {}", id, plan, months);
        planService.upgradePlan(id, com.example.medicinebox.enums.PlanType.fromString(plan), months);
        return BaseResponse.success("更新成功");
    }

    /**
     * 更新用户AI识别限额接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param id 用户ID
     * @param limit 新的AI识别月度限额
     * @return 操作结果，无返回数据
     */
    @PutMapping("/users/{id}/ai-limit")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> updateUserAiLimit(
            @PathVariable Integer id,
            @RequestParam Integer limit) {
        log.info("更新用户AI限额, ID: {}, limit: {}", id, limit);
        authService.updateMonthAiLimit(id, limit);
        return BaseResponse.success("更新成功");
    }

    /**
     * 管理员创建用户接口
     * HTTP方法：POST
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param request 创建用户请求体，包含手机号、密码、姓名等信息
     * @return 创建成功的用户详细信息
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<UserInfoResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        log.info("管理员创建用户, 手机号: {}", request.getPhone());
        User user = authService.createUserByAdmin(request);
        UserInfoResponse response = authService.getUserById(user.getId());
        return BaseResponse.success("创建成功", response);
    }

    /**
     * 删除用户接口（高危操作，需二次密码验证）
     * HTTP方法：DELETE
     * 权限要求：管理员角色（ADMIN）
     *
     * @param id 用户ID
     * @param adminPassword 管理员密码（二次验证）
     * @return 操作结果，无返回数据
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> deleteUser(
            @PathVariable Integer id,
            @RequestParam String adminPassword) {
        if (!verifyAdminPassword(adminPassword)) {
            return BaseResponse.error(403, "管理员密码验证失败");
        }
        log.info("删除用户, ID: {}", id);
        authService.deleteUser(id);
        return BaseResponse.success("删除成功");
    }

    /**
     * 封禁用户接口（高危操作，需二次密码验证）
     * HTTP方法：POST
     * 权限要求：管理员角色（ADMIN）
     *
     * @param id 用户ID
     * @param reason 封禁原因，默认"管理员封禁"
     * @param adminPassword 管理员密码（二次验证）
     * @return 操作结果，无返回数据
     */
    @PostMapping("/users/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> blockUser(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "管理员封禁") String reason,
            @RequestParam String adminPassword) {
        if (!verifyAdminPassword(adminPassword)) {
            return BaseResponse.error(403, "管理员密码验证失败");
        }
        log.info("封禁用户, ID: {}, reason: {}", id, reason);
        authService.blockUser(id, reason);
        return BaseResponse.success("封禁成功");
    }

    /**
     * 冻结用户接口（高危操作，需二次密码验证）
     * HTTP方法：POST
     * 权限要求：管理员角色（ADMIN）
     *
     * @param id 用户ID
     * @param adminPassword 管理员密码（二次验证）
     * @return 操作结果，无返回数据
     */
    @PostMapping("/users/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> freezeUser(
            @PathVariable Integer id,
            @RequestParam String adminPassword) {
        if (!verifyAdminPassword(adminPassword)) {
            return BaseResponse.error(403, "管理员密码验证失败");
        }
        log.info("冻结用户, ID: {}", id);
        authService.freezeUser(id);
        return BaseResponse.success("冻结成功");
    }

    /**
     * 解封用户接口（高危操作，需二次密码验证）
     * HTTP方法：POST
     * 权限要求：管理员角色（ADMIN）
     *
     * @param id 用户ID
     * @param adminPassword 管理员密码（二次验证）
     * @return 操作结果，无返回数据
     */
    @PostMapping("/users/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> unblockUser(
            @PathVariable Integer id,
            @RequestParam String adminPassword) {
        if (!verifyAdminPassword(adminPassword)) {
            return BaseResponse.error(403, "管理员密码验证失败");
        }
        log.info("解封用户, ID: {}", id);
        authService.unblockUser(id);
        return BaseResponse.success("解封成功");
    }

    /**
     * 验证管理员密码（内部方法）
     * 从 SecurityContext 获取当前管理员手机号，验证密码是否正确
     *
     * @param password 待验证的密码
     * @return 密码是否正确
     */
    private boolean verifyAdminPassword(String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String adminPhone = authentication.getName();
        return authService.verifyAdminPassword(adminPhone, password);
    }

    /**
     * 批量删除用户接口
     * HTTP方法：DELETE
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param ids 用户ID列表
     * @return 删除成功的用户数量
     */
    @DeleteMapping("/users/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Integer> batchDeleteUsers(@RequestBody List<Integer> ids) {
        log.info("批量删除用户, IDs: {}", ids);
        int deletedCount = authService.batchDeleteUsers(ids);
        return BaseResponse.success("批量删除完成，共删除 " + deletedCount + " 个用户", deletedCount);
    }

    /**
     * 批量更新用户状态接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param request 请求体，包含用户ID列表和目标状态
     * @return 更新成功的用户数量
     */
    @PutMapping("/users/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Integer> batchUpdateUserStatus(
            @RequestBody Map<String, Object> request) {
        List<Integer> ids = (List<Integer>) request.get("ids");
        String status = (String) request.get("status");
        log.info("批量更新用户状态, IDs: {}, status: {}", ids, status);
        int updatedCount = authService.batchUpdateUserStatus(ids, status);
        return BaseResponse.success("批量更新完成，共更新 " + updatedCount + " 个用户", updatedCount);
    }

    /**
     * 批量更新用户角色接口
     * HTTP方法：PUT
     * 权限要求：管理员角色（ADMIN）
     * 
     * @param request 请求体，包含用户ID列表和目标角色
     * @return 更新成功的用户数量
     */
    @PutMapping("/users/batch/role")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Integer> batchUpdateUserRole(
            @RequestBody Map<String, Object> request) {
        List<Integer> ids = (List<Integer>) request.get("ids");
        String role = (String) request.get("role");
        log.info("批量更新用户角色, IDs: {}, role: {}", ids, role);
        int updatedCount = authService.batchUpdateUserRole(ids, role);
        return BaseResponse.success("批量更新完成，共更新 " + updatedCount + " 个用户", updatedCount);
    }

    /**
     * 分页查询系统日志接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 支持按用户ID和模块筛选日志
     * 
     * @param userId 用户ID（可选）
     * @param module 日志模块（可选）
     * @param page 页码，默认0
     * @param size 每页数量，默认20
     * @return 日志列表和分页信息
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Map<String, Object>> getLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int validatedPage = PaginationUtil.validatePage(page);
        int validatedSize = PaginationUtil.validateSize(size);
        log.info("查询系统日志, userId: {}, module: {}, page: {}, size: {}", userId, module, validatedPage, validatedSize);
        Page<SystemLog> logPage;
        if (userId != null && module != null && !module.isEmpty()) {
            logPage = systemLogService.findByModuleAndUserId(module, userId, PageRequest.of(validatedPage, validatedSize));
        } else if (userId != null) {
            logPage = systemLogService.findByUserId(userId, PageRequest.of(validatedPage, validatedSize));
        } else if (module != null && !module.isEmpty()) {
            logPage = systemLogService.findByModule(module, PageRequest.of(validatedPage, validatedSize));
        } else {
            logPage = systemLogService.findAll(PageRequest.of(validatedPage, validatedSize));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", logPage.getContent());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        result.put("currentPage", page);
        return BaseResponse.success("获取成功", result);
    }

    /**
     * 获取日志模块列表接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @return 所有日志模块名称列表
     */
    @GetMapping("/logs/modules")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<java.util.List<String>> getLogModules() {
        log.info("获取日志模块列表");
        return BaseResponse.success("获取成功", systemLogService.findDistinctModules());
    }

    /**
     * 获取日志统计信息接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 返回总日志数、今日日志数、失败日志数等统计数据
     * 
     * @return 日志统计数据Map
     */
    @GetMapping("/logs/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Map<String, Object>> getLogStats() {
        log.info("获取日志统计信息");
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", systemLogService.countAllLogs());
        stats.put("todayCount", systemLogService.countTodayLogs());
        stats.put("failedCount", systemLogService.countFailedLogs());
        return BaseResponse.success("获取成功", stats);
    }
}
