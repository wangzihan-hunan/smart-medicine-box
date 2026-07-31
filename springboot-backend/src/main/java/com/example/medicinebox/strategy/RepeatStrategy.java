package com.example.medicinebox.strategy;

import java.time.LocalDateTime;

/**
 * 重复提醒策略接口
 * 不同提醒频率（每日/每周/每月）通过实现此接口来定义下次提醒时间的计算逻辑
 */
public interface RepeatStrategy {

    /**
     * 计算下次提醒时间
     *
     * @param currentRemindTime 当前提醒时间
     * @return 下次提醒时间
     */
    LocalDateTime calculateNext(LocalDateTime currentRemindTime);

    /** 策略是否支持指定频率 */
    boolean supports(String frequency);
}