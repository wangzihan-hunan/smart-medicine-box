package com.example.medicinebox.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重复提醒策略工厂
 * 根据提醒频率选择合适的策略来计算下次提醒时间
 * 新增频率类型只需添加策略实现类，无需修改工厂或调用方代码
 */
@Component
public class RepeatStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(RepeatStrategyFactory.class);

    private final List<RepeatStrategy> strategies;
    private final DailyRepeatStrategy defaultStrategy;

    public RepeatStrategyFactory(List<RepeatStrategy> strategies, DailyRepeatStrategy defaultStrategy) {
        this.strategies = strategies;
        this.defaultStrategy = defaultStrategy;
    }

    /**
     * 根据频率获取对应的策略，未匹配时回退到每日策略
     *
     * @param frequency 提醒频率
     * @return 匹配的策略
     */
    public RepeatStrategy getStrategy(String frequency) {
        for (RepeatStrategy strategy : strategies) {
            if (strategy.supports(frequency)) {
                return strategy;
            }
        }
        log.debug("未找到匹配的频率策略: {}, 回退到每日策略", frequency);
        return defaultStrategy;
    }

    /**
     * 根据频率计算下次提醒时间
     */
    public LocalDateTime calculateNext(String frequency, LocalDateTime currentRemindTime) {
        return getStrategy(frequency).calculateNext(currentRemindTime);
    }
}