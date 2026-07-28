package com.example.medicinebox.controller;

import com.example.medicinebox.dto.request.ChangePasswordRequest;
import com.example.medicinebox.dto.request.UserLoginRequest;
import com.example.medicinebox.dto.request.UserRegisterRequest;
import com.example.medicinebox.dto.request.CaptchaRequest;
import com.example.medicinebox.dto.request.ResetPasswordRequest;
import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.dto.response.LoginResponse;
import com.example.medicinebox.dto.response.RegisterResponse;
import com.example.medicinebox.dto.response.UserInfoResponse;
import com.example.medicinebox.dto.response.UserListResponse;
import com.example.medicinebox.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证与授权控制器
 * 处理用户注册、登录、个人信息管理、密码修改等认证相关功能
 * 路由前缀：/api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口
     * HTTP方法：POST
     * 权限要求：公开（无需登录）
     * 
     * @param request 用户注册请求体，包含手机号、密码、姓名等信息
     * @return 注册成功响应，包含用户ID等信息
     */
    @PostMapping("/register")
    public BaseResponse<RegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("用户注册请求, 手机号: {}", request.getPhone());
        RegisterResponse response = authService.register(request);
        log.info("用户注册成功, 用户ID: {}", response.getUserId());
        return BaseResponse.success("注册成功！去登录吧", response);
    }

    /**
     * 用户登录接口
     * HTTP方法：POST
     * 权限要求：公开（无需登录）
     * 
     * @param request 用户登录请求体，包含手机号和密码
     * @return 登录成功响应，包含访问令牌等信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("用户登录请求, 手机号: {}", request.getPhone());
        LoginResponse response = authService.login(request);
        log.info("用户登录成功, 手机号: {}", request.getPhone());
        return BaseResponse.success("登录成功！", response);
    }

    /**
     * 获取当前登录用户信息接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 当前用户的详细信息
     */
    @GetMapping("/me")
    public BaseResponse<UserInfoResponse> getCurrentUserInfo() {
        log.info("获取当前用户信息");
        UserInfoResponse response = authService.getCurrentUserInfo();
        return BaseResponse.success("获取用户信息成功！", response);
    }

    /**
     * 更新当前用户信息接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param name 用户姓名（可选参数）
     * @return 更新后的用户信息
     */
    @PutMapping("/me")
    public BaseResponse<UserInfoResponse> updateUserInfo(@RequestParam(required = false) String name) {
        log.info("更新用户信息, 姓名: {}", name);
        UserInfoResponse response = authService.updateUserInfo(name);
        log.info("更新用户信息成功, 用户ID: {}", response.getId());
        return BaseResponse.success("更新用户信息成功！", response);
    }

    /**
     * 修改用户密码接口
     * HTTP方法：PUT
     * 权限要求：已登录用户
     * 
     * @param request 修改密码请求体，包含旧密码和新密码
     * @return 操作结果，无返回数据
     */
    @PutMapping("/change-password")
    public BaseResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        log.info("修改密码请求");
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        log.info("密码修改成功");
        return BaseResponse.success("密码修改成功！");
    }

    /**
     * 获取所有用户列表接口
     * HTTP方法：GET
     * 权限要求：管理员角色（ADMIN）
     * 
     * @return 用户列表响应，包含用户总数和用户信息列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<UserListResponse> getAllUsers() {
        log.info("获取用户列表");
        UserListResponse response = authService.getAllUsers();
        log.info("获取用户列表成功, 数量: {}", response.getTotalElements());
        return BaseResponse.success("获取用户列表成功！", response);
    }

    /**
     * 发送验证码接口（找回密码）
     * HTTP方法：POST
     * 权限要求：公开（无需登录）
     * 
     * @param request 验证码请求体，包含手机号
     * @return 操作结果，无返回数据
     */
    @PostMapping("/send-captcha")
    public BaseResponse<Void> sendCaptcha(@Valid @RequestBody CaptchaRequest request) {
        log.info("发送验证码请求, 手机号: {}", request.getPhone());
        authService.sendCaptcha(request.getPhone());
        log.info("验证码发送成功, 手机号: {}", request.getPhone());
        return BaseResponse.success("验证码已发送，请注意查收短信！");
    }

    /**
     * 重置密码接口（通过验证码）
     * HTTP方法：PUT
     * 权限要求：公开（无需登录）
     * 
     * @param request 重置密码请求体，包含手机号、验证码和新密码
     * @return 操作结果，无返回数据
     */
    @PutMapping("/reset-password")
    public BaseResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("重置密码请求, 手机号: {}", request.getPhone());
        authService.resetPassword(request.getPhone(), request.getCaptcha(), request.getNewPassword());
        log.info("密码重置成功, 手机号: {}", request.getPhone());
        return BaseResponse.success("密码重置成功，请使用新密码登录！");
    }
}
