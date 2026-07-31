package com.example.medicinebox.service;

import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户统计服务类
 * 负责用户相关的统计数据分析，如仪表盘数据
 */
@Service
public class UserStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(UserStatisticsService.class);

    private final UserRepository userRepository;

    public UserStatisticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取用户统计数据（用于仪表盘）
     * 使用数据库聚合查询，一次查询获取所有统计数据，避免全量加载到内存
     *
     * @return 用户统计Map：totalUsers, activeUsers, lockedUsers, blockedUsers, suspendedUsers, adminUsers, paidUsers
     */
    public Map<String, Long> getUserStats() {
        log.debug("获取用户统计数据（聚合查询）");
        List<Object[]> result = userRepository.countUserStats();
        Map<String, Long> stats = new HashMap<>();

        if (!result.isEmpty()) {
            Object[] row = result.get(0);
            stats.put("totalUsers", toLong(row[0]));
            stats.put("activeUsers", toLong(row[1]));
            stats.put("lockedUsers", toLong(row[2]));
            stats.put("blockedUsers", toLong(row[3]));
            stats.put("suspendedUsers", toLong(row[4]));
            stats.put("adminUsers", toLong(row[5]));
            stats.put("paidUsers", toLong(row[6]));
        }

        return stats;
    }

    private long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}