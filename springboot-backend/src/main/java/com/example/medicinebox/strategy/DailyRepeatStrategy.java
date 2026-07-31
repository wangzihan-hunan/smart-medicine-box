package com.example.medicinebox.strategy;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每日重复策略：下次提醒 = 明天同一时间
 */
@Component
public class DailyRepeatStrategy implements RepeatStrategy {

    @Override
    public LocalDateTime calculateNext(LocalDateTime currentRemindTime) {
        return currentRemindTime.plusDays(1);
    }

    @Override
    public boolean supports(String frequency) {
        return "每日一次".equals(frequency) || "每日两次".equals(frequency) || "每日三次".equals(frequency);
    }
}