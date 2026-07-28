package com.example.medicinebox.service;

import com.example.medicinebox.entity.User;
import com.example.medicinebox.enums.PlanType;
import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户套餐管理服务类
 * 核心功能：套餐类型查询、套餐有效性校验、AI识别用量管理、套餐升级、月度用量重置等
 */
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private final UserRepository userRepository;

    /**
     * 构造函数，注入用户数据访问层
     *
     * @param userRepository 用户数据访问层
     */
    public PlanService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取用户当前套餐类型
     *
     * @param userId 用户ID
     * @return 套餐类型枚举，用户不存在返回免费版
     */
    public PlanType getUserPlan(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return PlanType.FREE;
        }
        return PlanType.fromString(user.getPlan());
    }

    /**
     * 检查用户套餐是否有效（未过期）
     * 免费版永久有效，付费版需检查过期时间
     *
     * @param userId 用户ID
     * @return true-套餐有效，false-已过期或用户不存在
     */
    public boolean hasActivePlan(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if ("FREE".equals(user.getPlan())) {
            return true; // 免费版永久有效
        }
        if (user.getPlanExpireAt() == null) {
            return false;
        }
        return user.getPlanExpireAt().isAfter(LocalDateTime.now());
    }

    /**
     * 检查用户是否可以使用AI识别（在月度限额内）
     * 专业版且套餐有效则不限次数，其他版本按月度限额判断
     *
     * @param userId 用户ID
     * @return true-可以使用，false-超出限额或用户不存在
     */
    public boolean canUseAiRecognition(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Reset monthly count if needed
        resetMonthlyCountIfNeeded(user);

        PlanType plan = PlanType.fromString(user.getPlan());
        if (plan == PlanType.PROFESSIONAL && hasActivePlan(userId)) {
            return true; // Unlimited for pro
        }

        return user.getMonthAiCount() < user.getMonthAiLimit();
    }

    /**
     * 获取用户剩余AI识别次数
     * 专业版且套餐有效返回无限次（Integer.MAX_VALUE）
     *
     * @param userId 用户ID
     * @return 剩余次数，用户不存在返回0
     */
    public int getRemainingAiCount(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }

        // 跨月时重置月度计数
        resetMonthlyCountIfNeeded(user);

        PlanType plan = PlanType.fromString(user.getPlan());
        // 专业版且套餐有效则不限次数
        if (plan == PlanType.PROFESSIONAL && hasActivePlan(userId)) {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, user.getMonthAiLimit() - user.getMonthAiCount());
    }

    /**
     * 消耗一次AI识别用量
     * 专业版且套餐有效则不扣减用量，直接返回成功
     *
     * @param userId 用户ID
     * @return true-消耗成功，false-超出限额或用户不存在
     */
    @Transactional
    public boolean consumeAiRecognition(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        resetMonthlyCountIfNeeded(user);

        PlanType plan = PlanType.fromString(user.getPlan());
        if (plan == PlanType.PROFESSIONAL && hasActivePlan(userId)) {
            log.debug("User {} is on PRO plan, unlimited AI recognition", userId);
            return true;
        }

        if (user.getMonthAiCount() >= user.getMonthAiLimit()) {
            log.warn("User {} exceeded monthly AI recognition limit: {}/{}",
                    userId, user.getMonthAiCount(), user.getMonthAiLimit());
            return false;
        }

        user.setMonthAiCount(user.getMonthAiCount() + 1);
        userRepository.save(user);

        log.info("User {} consumed AI recognition, remaining: {}/{}",
                userId, user.getMonthAiLimit() - user.getMonthAiCount(), user.getMonthAiLimit());
        return true;
    }

    /**
     * 升级用户套餐
     * 设置新的套餐类型、过期时间、AI限额，重置月度用量
     *
     * @param userId 用户ID
     * @param planType 新套餐类型
     * @param months 有效期（月）
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Transactional
    public void upgradePlan(Integer userId, PlanType planType, int months) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMonths(months);

        user.setPlan(planType.name());
        user.setPlanExpireAt(expireAt);
        user.setMonthAiLimit(planType.getAiRecognitionLimit());
        user.setMonthAiCount(0);
        user.setMonthResetAt(now);

        userRepository.save(user);

        log.info("User {} upgraded to {} plan, expires at {}", userId, planType, expireAt);
    }

    /**
     * 如果已进入新月份，重置月度AI识别用量
     *
     * @param user 用户实体
     */
    private void resetMonthlyCountIfNeeded(User user) {
        if (user.getMonthResetAt() == null) {
            user.setMonthResetAt(LocalDateTime.now());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getMonthResetAt().getMonthValue() != now.getMonthValue() ||
                user.getMonthResetAt().getYear() != now.getYear()) {
            log.info("Resetting monthly AI count for user {}, last reset: {}",
                    user.getId(), user.getMonthResetAt());
            user.setMonthAiCount(0);
            user.setMonthResetAt(now);
            userRepository.save(user);
        }
    }

    /**
     * 获取用户套餐详情
     * 包含套餐类型、名称、有效期、AI识别限额/已用/剩余、药品限额、提醒限额等
     *
     * @param userId 用户ID
     * @return 套餐详情对象，用户不存在返回null
     */
    public PlanInfo getPlanInfo(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        // 跨月时重置月度计数
        resetMonthlyCountIfNeeded(user);

        PlanType plan = PlanType.fromString(user.getPlan());
        PlanInfo info = new PlanInfo();
        info.setPlan(plan.name());
        info.setPlanName(plan.getDisplayName());
        info.setActive(hasActivePlan(userId));
        info.setExpireAt(user.getPlanExpireAt());
        // 无限额用-1表示
        info.setAiLimit(plan.getAiRecognitionLimit() == Integer.MAX_VALUE ? -1 : plan.getAiRecognitionLimit());
        info.setAiUsed(user.getMonthAiCount());
        info.setAiRemaining(getRemainingAiCount(userId));
        info.setMedicineLimit(plan.getMedicineLimit() == Integer.MAX_VALUE ? -1 : plan.getMedicineLimit());
        info.setReminderLimit(plan.getReminderLimit() == Integer.MAX_VALUE ? -1 : plan.getReminderLimit());

        return info;
    }

    /**
     * 套餐信息数据传输对象（DTO）
     * 封装套餐相关的所有信息用于前端展示
     */
    public static class PlanInfo {
        private String plan;
        private String planName;
        private boolean active;
        private LocalDateTime expireAt;
        private int aiLimit;
        private int aiUsed;
        private int aiRemaining;
        private int medicineLimit;
        private int reminderLimit;

        // Getters and Setters
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getExpireAt() { return expireAt; }
        public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
        public int getAiLimit() { return aiLimit; }
        public void setAiLimit(int aiLimit) { this.aiLimit = aiLimit; }
        public int getAiUsed() { return aiUsed; }
        public void setAiUsed(int aiUsed) { this.aiUsed = aiUsed; }
        public int getAiRemaining() { return aiRemaining; }
        public void setAiRemaining(int aiRemaining) { this.aiRemaining = aiRemaining; }
        public int getMedicineLimit() { return medicineLimit; }
        public void setMedicineLimit(int medicineLimit) { this.medicineLimit = medicineLimit; }
        public int getReminderLimit() { return reminderLimit; }
        public void setReminderLimit(int reminderLimit) { this.reminderLimit = reminderLimit; }
    }
}
