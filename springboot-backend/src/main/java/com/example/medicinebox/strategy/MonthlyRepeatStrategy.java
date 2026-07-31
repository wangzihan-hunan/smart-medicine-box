package com.example.medicinebox.strategy;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每月重复策略：下次提醒 = 下月同一天同一时间
 */
@Component
public class MonthlyRepeatStrategy implements RepeatStrategy {

    @Override
    public LocalDateTime calculateNext(LocalDateTime currentRemindTime) {
        return currentRemindTime.plusMonths(1);
    }

    @Override
    public boolean supports(String frequency) {
        return "每月一次".equals(frequency);
    }
}