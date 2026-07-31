package com.example.medicinebox.service;

import com.example.medicinebox.constant.BusinessConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

/**
 * 字母验证码服务类
 * 核心功能：生成随机字母验证码字符串、验证验证码
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    // 验证码字符集（仅字母，移除易混淆字符：O、o、I、l）
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    private final HttpSession session;

    public CaptchaService(HttpSession session) {
        this.session = session;
    }

    /**
     * 生成随机字母验证码字符串
     * 
     * @return 验证码字符串
     */
    public String generateCaptcha() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < BusinessConstants.CAPTCHA_LENGTH; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        String captcha = sb.toString();
        log.debug("生成验证码: {}", captcha);

        // 将验证码存储到Session
        session.setAttribute("captcha", captcha.toLowerCase());

        return captcha;
    }

    /**
     * 验证字母验证码
     * 
     * @param inputCaptcha 用户输入的验证码
     * @return 验证结果（true表示验证成功）
     */
    public boolean verifyCaptcha(String inputCaptcha) {
        if (inputCaptcha == null || inputCaptcha.isEmpty()) {
            log.warn("验证码为空");
            return false;
        }

        String storedCaptcha = (String) session.getAttribute("captcha");
        if (storedCaptcha == null) {
            log.warn("验证码已过期或未生成");
            return false;
        }

        // 验证后立即清除Session中的验证码（防止重复使用）
        session.removeAttribute("captcha");

        boolean isValid = storedCaptcha.equals(inputCaptcha.toLowerCase());
        log.debug("验证码验证, 用户输入: {}, 正确值: {}, 结果: {}", 
                inputCaptcha, storedCaptcha, isValid);
        
        return isValid;
    }
}
