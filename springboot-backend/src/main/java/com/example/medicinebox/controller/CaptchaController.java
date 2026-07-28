package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码控制器
 * 处理字母验证码相关功能
 * 路由前缀：/api/v1/captcha
 */
@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取字母验证码字符串
     * HTTP方法：GET
     * 权限要求：公开（无需登录）
     * 
     * @return 包含验证码字符串的JSON响应
     */
    @GetMapping("/text")
    public BaseResponse<Map<String, String>> getCaptchaText() {
        log.debug("获取字母验证码字符串");

        // 生成随机字母验证码
        String captcha = captchaService.generateCaptcha();

        // 返回验证码字符串
        Map<String, String> result = new HashMap<>();
        result.put("captcha", captcha);

        log.debug("字母验证码生成成功");
        return BaseResponse.success("获取验证码成功", result);
    }

    /**
     * 验证字母验证码
     * HTTP方法：POST
     * 权限要求：公开（无需登录）
     * 
     * @param request 包含用户输入验证码的请求体
     * @return 验证结果
     */
    @PostMapping("/verify")
    public BaseResponse<Boolean> verifyCaptcha(@RequestBody VerifyCaptchaRequest request) {
        log.debug("验证字母验证码");
        boolean isValid = captchaService.verifyCaptcha(request.getCaptcha());
        if (isValid) {
            log.debug("字母验证码验证成功");
            return BaseResponse.success("验证成功！", true);
        } else {
            log.warn("字母验证码验证失败");
            return BaseResponse.success("验证码错误！", false);
        }
    }

    /**
     * 验证验证码请求内部类
     */
    public static class VerifyCaptchaRequest {
        private String captcha;

        public String getCaptcha() {
            return captcha;
        }

        public void setCaptcha(String captcha) {
            this.captcha = captcha;
        }
    }
}
