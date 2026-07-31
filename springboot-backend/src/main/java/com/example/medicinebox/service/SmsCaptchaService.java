package com.example.medicinebox.service;

import com.example.medicinebox.config.AuthProperties;
import com.example.medicinebox.dto.CaptchaInfo;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.BadRequestException;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务类
 * 负责短信验证码的生成、发送、验证，以及通过验证码重置密码
 * 验证码存储使用 Redis，支持过期自动清理和分布式部署
 */
@Service
public class SmsCaptchaService {

    private static final Logger log = LoggerFactory.getLogger(SmsCaptchaService.class);

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    public SmsCaptchaService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             AuthProperties authProperties, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发送验证码（用于找回密码）
     *
     * @param phone 接收验证码的手机号
     * @throws BadRequestException 用户不存在或发送过于频繁时抛出
     */
    public void sendCaptcha(String phone) {
        log.debug("发送验证码, 手机号: {}", PhoneUtils.maskPhone(phone));

        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            log.warn("用户不存在, 无法发送验证码: {}", PhoneUtils.maskPhone(phone));
            throw new BadRequestException("该手机号未注册");
        }

        String redisKey = CAPTCHA_KEY_PREFIX + phone;
        CaptchaInfo cached = (CaptchaInfo) redisTemplate.opsForValue().get(redisKey);

        // 检查发送频率
        if (cached != null) {
            long intervalMillis = authProperties.getCaptchaSendIntervalSeconds() * 1000L;
            if (System.currentTimeMillis() - cached.getSendTime() < intervalMillis) {
                log.warn("验证码发送过于频繁: {}", PhoneUtils.maskPhone(phone));
                throw new BadRequestException("验证码发送过于频繁，请稍后再试");
            }
        }

        String captcha = generateCaptcha();
        long expireTime = System.currentTimeMillis() + authProperties.getCaptchaExpireMinutes() * 60_000L;
        CaptchaInfo captchaInfo = new CaptchaInfo(captcha, expireTime, System.currentTimeMillis());

        redisTemplate.opsForValue().set(redisKey, captchaInfo,
                authProperties.getCaptchaExpireMinutes(), TimeUnit.MINUTES);

        log.info("验证码已发送, 手机号: {}, 验证码: {} (模拟)", PhoneUtils.maskPhone(phone), captcha);
    }

    /**
     * 通过验证码重置密码
     *
     * @param phone 手机号
     * @param captcha 验证码
     * @param newPassword 新密码（明文）
     * @throws BadRequestException 验证码无效、过期或用户不存在时抛出
     */
    @Transactional
    public void resetPassword(String phone, String captcha, String newPassword) {
        log.debug("重置密码, 手机号: {}", PhoneUtils.maskPhone(phone));

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException("该手机号未注册"));

        String redisKey = CAPTCHA_KEY_PREFIX + phone;
        CaptchaInfo cached = (CaptchaInfo) redisTemplate.opsForValue().get(redisKey);

        if (cached == null) {
            log.warn("验证码不存在, 手机号: {}", PhoneUtils.maskPhone(phone));
            throw new BadRequestException("验证码不存在，请重新获取");
        }

        if (cached.isExpired()) {
            log.warn("验证码已过期, 手机号: {}", PhoneUtils.maskPhone(phone));
            redisTemplate.delete(redisKey);
            throw new BadRequestException("验证码已过期，请重新获取");
        }

        if (!captcha.equals(cached.getCaptcha())) {
            log.warn("验证码错误, 手机号: {}", PhoneUtils.maskPhone(phone));
            throw new BadRequestException("验证码错误，请重新输入");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginAttempts(0);
        userRepository.save(user);

        redisTemplate.delete(redisKey);
        log.info("密码重置成功, 用户ID: {}", user.getId());
    }

    private String generateCaptcha() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }
}