package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.ReminderCreateRequest;
import com.example.medicinebox.dto.response.ReminderListResponse;
import com.example.medicinebox.dto.response.ReminderResponse;
import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用药提醒服务类
 * 核心功能：用药提醒的增删改查、标记已服用、今日提醒查询、定时触发提醒任务等
 */
@Service
public class ReminderService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    /**
     * 构造函数，注入依赖
     *
     * @param reminderRepository 提醒数据访问层
     * @param userRepository 用户数据访问层
     */
    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
    }

    /**
     * 添加用药提醒
     * 创建新的提醒记录并关联到当前用户
     *
     * @param request 提醒创建请求（药品ID、药品名称、时间、剂量、频次等）
     * @return 提醒响应对象
     */
    @Transactional
    public ReminderResponse addReminder(ReminderCreateRequest request) {
        User user = getCurrentUser();
        log.debug("添加提醒, 用户ID: {}, 药品名称: {}, 时间: {}", user.getId(), request.getMedicineName(), request.getTime());

        Reminder reminder = new Reminder();
        reminder.setMedicineId(request.getMedicineId());
        reminder.setMedicineName(request.getMedicineName());
        reminder.setPrescriptionId(request.getPrescriptionId());
        reminder.setTime(request.getTime());
        reminder.setDosage(request.getDosage());
        reminder.setFrequency(request.getFrequency());
        reminder.setIsRepeat(request.getIsRepeat());
        reminder.setRemark(request.getRemark());
        reminder.setStartDate(request.getStartDate());
        reminder.setEndDate(request.getEndDate());
        reminder.setRepeatEndType(request.getRepeatEndType());
        reminder.setRepeatEndDate(request.getRepeatEndDate());
        reminder.setRepeatCount(request.getRepeatCount());
        reminder.setAdvanceNotice(request.getAdvanceNotice());
        reminder.setSoundEffect(request.getSoundEffect());
        reminder.setUserId(user.getId());

        reminder = reminderRepository.save(reminder);

        log.debug("添加提醒成功, 提醒ID: {}", reminder.getId());
        return ReminderResponse.fromEntity(reminder);
    }

    /**
     * 分页查询今日用药提醒
     * 按时间升序排列
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页提醒列表响应
     */
    public ReminderListResponse getTodayReminders(int page, int size) {
        User user = getCurrentUser();
        log.debug("查询今日提醒, 用户ID: {}, 页码: {}, 大小: {}", user.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "time"));
        LocalDate today = LocalDate.now();
        Page<Reminder> reminderPage = reminderRepository.findByUserIdAndDate(
                user.getId(), today, pageable);
        
        List<ReminderResponse> responseList = reminderPage.getContent().stream()
                .map(ReminderResponse::fromEntity)
                .collect(Collectors.toList());

        log.debug("查询提醒列表成功, 用户ID: {}, 总数: {}", user.getId(), reminderPage.getTotalElements());
        return new ReminderListResponse(
                (int) reminderPage.getTotalElements(),
                reminderPage.getTotalPages(),
                reminderPage.getNumber(),
                responseList
        );
    }

    /**
     * 标记提醒为已服用
     *
     * @param id 提醒ID
     * @return 更新后的提醒响应对象
     * @throws ResourceNotFoundException 提醒不存在或无权限时抛出
     */
    @Transactional
    public ReminderResponse markTaken(Integer id) {
        User user = getCurrentUser();
        log.debug("标记已服用, 用户ID: {}, 提醒ID: {}", user.getId(), id);
        
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.warn("提醒不存在, 用户ID: {}, 提醒ID: {}", user.getId(), id);
                    return new ResourceNotFoundException("提醒不存在");
                });
        
        reminder.setTaken(true);
        reminder = reminderRepository.save(reminder);
        
        log.debug("标记已服用成功, 提醒ID: {}", reminder.getId());
        return ReminderResponse.fromEntity(reminder);
    }

    /**
     * 更新用药提醒
     * 仅更新非空字段，校验提醒归属权
     *
     * @param id 提醒ID
     * @param request 提醒更新请求
     * @return 更新后的提醒响应对象
     * @throws ResourceNotFoundException 提醒不存在或无权限时抛出
     */
    @Transactional
    public ReminderResponse updateReminder(Integer id, ReminderCreateRequest request) {
        User user = getCurrentUser();
        log.debug("更新提醒, 用户ID: {}, 提醒ID: {}", user.getId(), id);
        
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.warn("提醒不存在, 用户ID: {}, 提醒ID: {}", user.getId(), id);
                    return new ResourceNotFoundException("提醒不存在");
                });
        
        // 逐个字段非空判断，仅更新有值的字段
        if (request.getMedicineId() != null) {
            reminder.setMedicineId(request.getMedicineId());
        }
        if (request.getMedicineName() != null) {
            reminder.setMedicineName(request.getMedicineName());
        }
        if (request.getPrescriptionId() != null) {
            reminder.setPrescriptionId(request.getPrescriptionId());
        }
        if (request.getTime() != null) {
            reminder.setTime(request.getTime());
        }
        if (request.getDosage() != null) {
            reminder.setDosage(request.getDosage());
        }
        if (request.getFrequency() != null) {
            reminder.setFrequency(request.getFrequency());
        }
        if (request.getIsRepeat() != null) {
            reminder.setIsRepeat(request.getIsRepeat());
        }
        if (request.getRemark() != null) {
            reminder.setRemark(request.getRemark());
        }
        if (request.getStartDate() != null) {
            reminder.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            reminder.setEndDate(request.getEndDate());
        }
        if (request.getRepeatEndType() != null) {
            reminder.setRepeatEndType(request.getRepeatEndType());
        }
        if (request.getRepeatEndDate() != null) {
            reminder.setRepeatEndDate(request.getRepeatEndDate());
        }
        if (request.getRepeatCount() != null) {
            reminder.setRepeatCount(request.getRepeatCount());
        }
        if (request.getAdvanceNotice() != null) {
            reminder.setAdvanceNotice(request.getAdvanceNotice());
        }
        if (request.getSoundEffect() != null) {
            reminder.setSoundEffect(request.getSoundEffect());
        }

        reminder = reminderRepository.save(reminder);
        log.debug("更新提醒成功, 提醒ID: {}", reminder.getId());
        return ReminderResponse.fromEntity(reminder);
    }

    /**
     * 删除用药提醒
     * 校验提醒归属权，只能删除自己的提醒
     *
     * @param id 提醒ID
     * @throws ResourceNotFoundException 提醒不存在或无权限时抛出
     */
    @Transactional
    public void deleteReminder(Integer id) {
        User user = getCurrentUser();
        log.debug("删除提醒, 用户ID: {}, 提醒ID: {}", user.getId(), id);
        
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> {
                    log.warn("提醒不存在, 用户ID: {}, 提醒ID: {}", user.getId(), id);
                    return new ResourceNotFoundException("提醒不存在");
                });
        
        reminderRepository.delete(reminder);
        log.debug("删除提醒成功, 提醒ID: {}", id);
    }

    /**
     * 定时检查待触发的用药提醒 - 每分钟执行
     * 触发条件：提醒时间等于当前时间（HH:mm），且未服用，且在有效日期范围内
     * 非重复提醒触发后自动标记为已完成
     */
    @Scheduled(cron = "0 * * * * *")
    public void triggerReminders() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now();
        
        log.debug("检查用药提醒, 当前时间: {}", currentTime);
        
        List<Reminder> reminders = reminderRepository.findAll();
        int triggered = 0;
        
        for (Reminder reminder : reminders) {
            if (Boolean.TRUE.equals(reminder.getTaken())) {
                continue;
            }
            if (reminder.getTime() == null || !reminder.getTime().equals(currentTime)) {
                continue;
            }
            // 检查日期范围
            if (reminder.getStartDate() != null && today.isBefore(reminder.getStartDate())) {
                continue;
            }
            if (reminder.getEndDate() != null && today.isAfter(reminder.getEndDate())) {
                continue;
            }
            
            // 触发提醒（记录日志，实际推送由通知服务处理）
            User user = userRepository.findById(reminder.getUserId()).orElse(null);
            if (user != null) {
                log.info("触发用药提醒: 用户={}, 药品={}, 时间={}, 剂量={}",
                        user.getPhone(), reminder.getMedicineName(), reminder.getTime(), reminder.getDosage());
                triggered++;
                
                // 非重复提醒触发后自动标记为已完成
                if (!Boolean.TRUE.equals(reminder.getIsRepeat())) {
                    reminder.setTaken(true);
                    reminderRepository.save(reminder);
                }
            }
        }
        
        if (triggered > 0) {
            log.info("用药提醒触发完成, 共触发 {} 条提醒", triggered);
        }
    }
}