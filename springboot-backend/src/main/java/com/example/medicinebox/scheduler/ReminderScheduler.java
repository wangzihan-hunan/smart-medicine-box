package com.example.medicinebox.scheduler;

import com.example.medicinebox.entity.Reminder;
import com.example.medicinebox.repository.ReminderRepository;
import com.example.medicinebox.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 用药提醒定时任务调度器
 *
 * <p>负责扫描即将到期的用药提醒，并通过通知服务发送推送。
 * 支持以下特性：
 * <ul>
 *   <li>基于 nextRemindTime 字段的精确扫描</li>
 *   <li>按频率（每日/每周/每月）动态计算下一次提醒时间</li>
 *   <li>支持结束条件：永不结束、结束于指定日期、重复N次</li>
 *   <li>支持提前通知时间（advanceNotice）</li>
 * </ul>
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每30秒扫描一次到期提醒
     *
     * <p>扫描条件：激活状态、未服用、nextRemindTime <= 当前时间
     */
    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void checkDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("扫描到期提醒, 当前时间: {}", now);

        List<Reminder> dueReminders = reminderRepository.findByIsActiveTrueAndTakenFalseAndNextRemindTimeLessThanEqual(now);

        if (dueReminders.isEmpty()) {
            return;
        }

        log.info("发现 {} 条到期提醒", dueReminders.size());

        for (Reminder reminder : dueReminders) {
            try {
                // 检查是否在有效日期范围内
                if (!isInValidDateRange(reminder)) {
                    log.debug("提醒不在有效日期范围内, 跳过: {}", reminder.getId());
                    updateNextRemindTime(reminder, true);
                    continue;
                }

                // 检查重复结束条件
                if (isRepeatEnded(reminder)) {
                    log.debug("提醒已达到结束条件, 跳过: {}", reminder.getId());
                    reminder.setTaken(true);
                    reminderRepository.save(reminder);
                    continue;
                }

                // 发送通知
                notificationService.sendNotification(
                    reminder.getUserId(),
                    "用药提醒：" + reminder.getMedicineName(),
                    "到达服药时间，请按时服用 " + reminder.getDosage(),
                    "MEDICINE_REMINDER"
                );

                // 增加已触发次数并更新下次提醒时间
                reminder.setTriggeredCount((reminder.getTriggeredCount() == null ? 0 : reminder.getTriggeredCount()) + 1);
                updateNextRemindTime(reminder, false);

                log.debug("提醒已发送, 用户ID: {}, 药品: {}", reminder.getUserId(), reminder.getMedicineName());
            } catch (Exception e) {
                log.error("发送提醒失败, 提醒ID: {}, 错误: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    /**
     * 判断提醒是否在有效日期范围内
     *
     * @param reminder 提醒实体
     * @return 是否在有效范围内
     */
    private boolean isInValidDateRange(Reminder reminder) {
        LocalDate today = LocalDate.now();
        if (reminder.getStartDate() != null && today.isBefore(reminder.getStartDate())) {
            return false;
        }
        if (reminder.getEndDate() != null && today.isAfter(reminder.getEndDate())) {
            return false;
        }
        return true;
    }

    /**
     * 判断重复提醒是否已达到结束条件
     *
     * @param reminder 提醒实体
     * @return 是否已结束
     */
    private boolean isRepeatEnded(Reminder reminder) {
        String endType = reminder.getRepeatEndType();
        if (endType == null || "never".equals(endType)) {
            return false;
        }
        if ("endDate".equals(endType)) {
            return reminder.getRepeatEndDate() != null && LocalDate.now().isAfter(reminder.getRepeatEndDate());
        }
        if ("count".equals(endType)) {
            int count = reminder.getRepeatCount() == null ? 0 : reminder.getRepeatCount();
            int triggered = reminder.getTriggeredCount() == null ? 0 : reminder.getTriggeredCount();
            return triggered >= count;
        }
        return false;
    }

    /**
     * 更新提醒的下次提醒时间
     *
     * @param reminder 提醒实体
     * @param skipRepeat 是否跳过重复计算（直接设置为未来很远的时间）
     */
    private void updateNextRemindTime(Reminder reminder, boolean skipRepeat) {
        LocalDateTime nextTime;

        if (skipRepeat || !Boolean.TRUE.equals(reminder.getIsRepeat())) {
            // 非重复提醒或需要跳过：设置为100年后，实际上不再触发
            nextTime = LocalDateTime.now().plusYears(100);
        } else {
            // 根据频率计算下一次提醒时间
            nextTime = calculateNextRemindTime(reminder);
        }

        reminder.setNextRemindTime(nextTime);
        reminderRepository.save(reminder);
    }

    /**
     * 根据提醒频率计算下一次提醒时间
     *
     * <p>支持的频率格式：
     * <ul>
     *   <li>每日一次/每日两次/每日三次 -> 加1天</li>
     *   <li>每周一次/每周两次 -> 加7天</li>
     *   <li>每月一次/每月两次 -> 加1个月</li>
     * </ul>
     *
     * @param reminder 提醒实体
     * @return 下一次提醒时间
     */
    private LocalDateTime calculateNextRemindTime(Reminder reminder) {
        LocalDateTime baseTime = reminder.getNextRemindTime();
        if (baseTime == null) {
            baseTime = LocalDateTime.now();
        }

        String frequency = reminder.getFrequency();
        if (frequency == null) frequency = "每日一次";

        LocalDateTime nextTime;
        if (frequency.contains("每日")) {
            nextTime = baseTime.plusDays(1);
        } else if (frequency.contains("每周")) {
            nextTime = baseTime.plusWeeks(1);
        } else if (frequency.contains("每月")) {
            nextTime = baseTime.plusMonths(1);
        } else {
            // 默认每日一次
            nextTime = baseTime.plusDays(1);
        }

        // 如果计算出的时间已过，且频率是每日，则继续向后推进直到未来
        if (frequency.contains("每日") && !nextTime.isAfter(LocalDateTime.now())) {
            LocalDate today = LocalDate.now();
            LocalTime remindTime = extractRemindTime(reminder.getTime());
            nextTime = LocalDateTime.of(today.plusDays(1), remindTime);
        }

        return nextTime;
    }

    /**
     * 从时间字符串中提取 LocalTime
     *
     * @param time 时间字符串（如 "08:00"）
     * @return LocalTime 对象
     */
    private LocalTime extractRemindTime(String time) {
        try {
            if (time != null && time.trim().length() == 5) {
                return LocalTime.parse(time.trim());
            }
        } catch (Exception e) {
            // 解析失败时使用默认时间
        }
        return LocalTime.of(8, 0);
    }
}
