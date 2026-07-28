package com.example.medicinebox.config;

import com.example.medicinebox.entity.User;
import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 *
 * <p>实现CommandLineRunner接口，在Spring Boot应用启动完成后自动执行。
 * 主要用于初始化系统默认管理员账号、测试用户账号及少许测试数据，确保系统首次部署后可以直接使用。
 *
 * <p>注意：此初始化器仅用于开发/测试环境，生产环境应移除或修改默认密码。
 *
 * @author medicine-box
 * @since 1.0.0
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createDefaultAdminIfNotExists();
        createTestUserIfNotExists();
    }

    /**
     * 创建默认管理员账号（如果不存在）
     *
     * <p>默认管理员账号信息：
     * <ul>
     *   <li>手机号：13800138000</li>
     *   <li>密码：admin123（首次登录后强制修改）</li>
     *   <li>角色：ADMIN（管理员）</li>
     *   <li>套餐：PROFESSIONAL（专业版）</li>
     *   <li>状态：ACTIVE（激活）</li>
     *   <li>mustChangePassword：true（首次登录强制修改密码）</li>
     * </ul>
     */
    private void createDefaultAdminIfNotExists() {
        String adminPhone = "13800138000";
        String adminPassword = "admin123";

        User existingUser = userRepository.findByPhone(adminPhone).orElse(null);

        if (existingUser != null) {
            // 账号已存在，仅更新角色和套餐信息（不覆盖密码）
            log.info("管理员账号已存在, 手机号: {}", maskPhone(adminPhone));
            existingUser.setRole("ADMIN");
            existingUser.setStatus("ACTIVE");
            existingUser.setPlan("PROFESSIONAL");
            existingUser.setPlanExpireAt(LocalDateTime.now().plusYears(1));
            existingUser.setMonthAiLimit(9999);
            // 如果已经是管理员且已改过密码，不重置 mustChangePassword
            if (existingUser.getMustChangePassword() == null) {
                existingUser.setMustChangePassword(false);
            }
            userRepository.save(existingUser);
            return;
        }

        // 创建新的管理员账号
        log.info("创建默认管理员账号, 手机号: {}", maskPhone(adminPhone));

        User admin = new User();
        admin.setPhone(adminPhone);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setName("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setPhoneVerified(true);
        admin.setPlan("PROFESSIONAL");
        admin.setPlanExpireAt(LocalDateTime.now().plusYears(1));
        admin.setMonthAiLimit(9999);
        admin.setMustChangePassword(true); // 强制首次登录修改密码

        userRepository.save(admin);

        log.info("默认管理员账号创建成功, 手机号: {}", maskPhone(adminPhone));
        log.info("请使用初始密码登录后立即修改密码");
    }

    /**
     * 创建测试用户账号（如果不存在）
     *
     * <p>测试用户账号信息：
     * <ul>
     *   <li>手机号：13900139000</li>
     *   <li>密码：user123</li>
     *   <li>角色：USER（普通用户）</li>
     *   <li>套餐：BASIC（基础版）</li>
     *   <li>状态：ACTIVE（激活）</li>
     * </ul>
     */
    private void createTestUserIfNotExists() {
        String userPhone = "13900139000";
        String userPassword = "user123";

        User existingUser = userRepository.findByPhone(userPhone).orElse(null);

        if (existingUser != null) {
            log.info("测试用户账号已存在, 手机号: {}", maskPhone(userPhone));
            return;
        }

        log.info("创建测试用户账号, 手机号: {}", maskPhone(userPhone));

        User user = new User();
        user.setPhone(userPhone);
        user.setPassword(passwordEncoder.encode(userPassword));
        user.setName("测试用户");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setPhoneVerified(true);
        user.setPlan("BASIC");
        user.setPlanExpireAt(LocalDateTime.now().plusMonths(1));
        user.setMonthAiLimit(50);
        user.setMustChangePassword(false);

        userRepository.save(user);

        log.info("测试用户账号创建成功, 手机号: {}", maskPhone(userPhone));
    }

    /**
     * 手机号脱敏处理
     * 将手机号中间4位替换为****，用于日志输出时保护用户隐私
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
