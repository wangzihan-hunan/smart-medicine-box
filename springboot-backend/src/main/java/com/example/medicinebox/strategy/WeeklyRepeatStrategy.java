package com.example.medicinebox.strategy;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每周重复策略：下次提醒 = 下周同一天同一时间
 */
@Component
public class WeeklyRepeatStrategy implements RepeatStrategy {

    @Override
    public LocalDateTime calculateNext(LocalDateTime currentRemindTime) {
        return currentRemindTime.plusWeeks(1);
    }

    @Override
    public boolean supports(String frequency) {
        return "每周一次".equals(frequency) || "每周两次".equals(frequency);
    }
}