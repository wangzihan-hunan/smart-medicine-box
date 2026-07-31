package com.example.medicinebox.service;

import com.example.medicinebox.dto.request.ReminderCreateRequest;
import com.example.medicinebox.dto.response.ReminderListResponse;
import com.example.medicinebox.dto.response.ReminderResponse;
import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.entity.User;
import com.example.medicinebox.exception.ResourceNotFoundException;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.repository.UserRepository;
import com.example.medicinebox.strategy.RepeatStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 用药提醒服务类
 * 核心功能：用药提醒的增删改查、标记已服用、今日提醒查询、定时触发提醒任务等
 */
@Service
public class ReminderService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    /** 定时任务每批处理数量上限 */
    private static final int TRIGGER_BATCH_SIZE = 100;

    /** JVM级别并发锁，防止同一实例内定时任务重复执行 */
    private final ReentrantLock triggerLock = new ReentrantLock();

    private final ReminderRepository reminderRepository;
    private final RepeatStrategyFactory repeatStrategyFactory;

    /**
     * 构造函数，注入依赖
     *
     * @param reminderRepository 提醒数据访问层
     * @param userRepository 用户数据访问层
     * @param repeatStrategyFactory 重复策略工厂
     */
    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository,
                           RepeatStrategyFactory repeatStrategyFactory) {
        super(userRepository);
        this.reminderRepository = reminderRepository;
        this.repeatStrategyFactory = repeatStrategyFactory;
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
        BeanUtils.copyProperties(request, reminder);
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
        
        reminder.markTaken();
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
        
        // 只复制非空字段，保持原有字段值不变
        BeanUtils.copyProperties(request, reminder, getNullPropertyNames(request));

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
     * 使用分页查询避免大数据量内存溢出，使用 ReentrantLock 防止并发执行
     */
    @Scheduled(cron = "0 * * * * *")
    public void triggerReminders() {
        if (!triggerLock.tryLock()) {
            log.debug("上一次定时任务尚未完成，跳过本次触发");
            return;
        }
        try {
            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate today = LocalDate.now();
            
            log.debug("检查用药提醒, 当前时间: {}", currentTime);
            
            int page = 0;
            boolean hasMore = true;
            int triggered = 0;
            
            while (hasMore) {
                Pageable pageable = PageRequest.of(page, TRIGGER_BATCH_SIZE);
                Page<Reminder> reminderPage = reminderRepository
                        .findByTimeAndIsActiveTrueAndTakenFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                currentTime, today, today, pageable);
                
                if (reminderPage.isEmpty()) {
                    hasMore = false;
                    continue;
                }
                
                for (Reminder reminder : reminderPage.getContent()) {
                    // 处理重复提醒结束条件（领域逻辑在实体中）
                    if (reminder.shouldStopRepeat()) {
                        reminderRepository.save(reminder);
                        continue;
                    }
                    
                    User user = userRepository.findById(reminder.getUserId()).orElse(null);
                    if (user != null) {
                        log.info("触发用药提醒: 用户={}, 药品={}, 时间={}, 剂量={}",
                                user.getPhone(), reminder.getMedicineName(), reminder.getTime(), reminder.getDosage());
                        triggered++;
                        
                        if (!Boolean.TRUE.equals(reminder.getIsRepeat())) {
                            // 非重复提醒触发后自动标记为已完成
                            reminder.markTaken();
                            reminderRepository.save(reminder);
                        } else {
                            // 重复提醒：根据频率策略计算下次提醒时间
                            reminder.setNextRemindTime(
                                    repeatStrategyFactory.calculateNext(
                                            reminder.getFrequency(), reminder.getNextRemindTime()));
                            reminderRepository.save(reminder);
                        }
                    }
                }
                
                page++;
                hasMore = reminderPage.hasNext();
            }
            
            if (triggered > 0) {
                log.info("用药提醒触发完成, 共触发 {} 条提醒", triggered);
            }
        } catch (Exception e) {
            log.error("定时提醒任务执行异常", e);
        } finally {
            triggerLock.unlock();
        }
    }

    /**
     * 获取对象中值为 null 的属性名数组
     * 用于 BeanUtils.copyProperties 时跳过 null 字段
     *
     * @param source 源对象
     * @return null 属性名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(new String[0]);
    }
}