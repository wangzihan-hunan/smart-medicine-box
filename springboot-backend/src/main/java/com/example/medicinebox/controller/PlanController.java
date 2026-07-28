package com.example.medicinebox.controller;

import com.example.medicinebox.dto.response.BaseResponse;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.enums.PlanType;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户套餐管理控制器
 * 提供套餐查询、套餐列表、套餐升级等功能
 * 路由前缀：/api/v1/plans
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanService planService;
    private final UserRepository userRepository;

    public PlanController(PlanService planService, UserRepository userRepository) {
        this.planService = planService;
        this.userRepository = userRepository;
    }

    /**
     * 获取当前用户套餐信息接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 
     * @return 当前用户的套餐详细信息
     */
    @GetMapping("/my")
    public BaseResponse<PlanService.PlanInfo> getMyPlan() {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return BaseResponse.error(401, "请先登录");
        }

        PlanService.PlanInfo planInfo = planService.getPlanInfo(userId);
        if (planInfo == null) {
            return BaseResponse.error(404, "用户不存在");
        }

        return BaseResponse.success("查询成功", planInfo);
    }

    /**
     * 获取所有可用套餐列表接口
     * HTTP方法：GET
     * 权限要求：已登录用户
     * 返回免费版、基础版、专业版三种套餐的详细信息
     * 
     * @return 套餐信息Map，包含各套餐的名称、价格、限额等
     */
    @GetMapping
    public BaseResponse<Map<String, Object>> getAllPlans() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> free = new HashMap<>();
        free.put("name", PlanType.FREE.name());
        free.put("displayName", PlanType.FREE.getDisplayName());
        free.put("price", PlanType.FREE.getPriceCents() / 100.0);
        free.put("aiLimit", PlanType.FREE.getAiRecognitionLimit());
        free.put("medicineLimit", PlanType.FREE.getMedicineLimit());
        free.put("reminderLimit", PlanType.FREE.getReminderLimit());

        Map<String, Object> basic = new HashMap<>();
        basic.put("name", PlanType.BASIC.name());
        basic.put("displayName", PlanType.BASIC.getDisplayName());
        basic.put("price", PlanType.BASIC.getPriceCents() / 100.0);
        basic.put("aiLimit", PlanType.BASIC.getAiRecognitionLimit());
        basic.put("medicineLimit", PlanType.BASIC.getMedicineLimit());
        basic.put("reminderLimit", PlanType.BASIC.getReminderLimit());

        Map<String, Object> pro = new HashMap<>();
        pro.put("name", PlanType.PROFESSIONAL.name());
        pro.put("displayName", PlanType.PROFESSIONAL.getDisplayName());
        pro.put("price", PlanType.PROFESSIONAL.getPriceCents() / 100.0);
        pro.put("aiLimit", PlanType.PROFESSIONAL.getAiRecognitionLimit());
        pro.put("medicineLimit", PlanType.PROFESSIONAL.getMedicineLimit());
        pro.put("reminderLimit", PlanType.PROFESSIONAL.getReminderLimit());

        result.put("FREE", free);
        result.put("BASIC", basic);
        result.put("PROFESSIONAL", pro);

        return BaseResponse.success("查询成功", result);
    }

    /**
     * 升级用户套餐接口
     * HTTP方法：POST
     * 权限要求：已登录用户
     * 注意：当前为测试版本，实际使用需接入支付系统
     * 
     * @param plan 目标套餐类型（FREE/BASIC/PROFESSIONAL）
     * @param months 套餐时长，默认1个月
     * @return 操作结果，无返回数据
     */
    @PostMapping("/upgrade")
    public BaseResponse<Void> upgradePlan(@RequestParam("plan") String plan,
                                          @RequestParam(value = "months", defaultValue = "1") int months) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return BaseResponse.error(401, "请先登录");
        }

        try {
            PlanType planType = PlanType.fromString(plan);
            planService.upgradePlan(userId, planType, months);
            log.info("User {} upgraded to {} plan for {} months", userId, planType, months);
            return BaseResponse.success("升级成功");
        } catch (IllegalArgumentException e) {
            return BaseResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID
     * 私有辅助方法，从SecurityContext中获取当前登录用户的ID
     * 
     * @return 当前用户ID，若未登录则返回null
     */
    private Integer getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String phone = userDetails.getUsername();
                return userRepository.findByPhone(phone)
                        .map(User::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }
}
