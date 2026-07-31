package com.example.medicinebox.service;

import com.example.medicinebox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 批量用户操作服务类
 * 负责批量删除、批量更新状态、批量更新角色等批量操作
 * 使用 JPQL 批量更新/删除语句，避免 N+1 查询问题
 */
@Service
public class BatchUserService {

    private static final Logger log = LoggerFactory.getLogger(BatchUserService.class);

    private final UserRepository userRepository;

    public BatchUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 批量删除用户
     * 仅删除非管理员角色的用户
     *
     * @param ids 用户ID列表
     * @return 实际删除的用户数量
     */
    @Transactional
    public int batchDeleteUsers(List<Integer> ids) {
        log.debug("批量删除用户, IDs: {}", ids);
        int deletedCount = userRepository.batchDelete(ids);
        log.info("批量删除用户完成, 删除数量: {}", deletedCount);
        return deletedCount;
    }

    /**
     * 批量更新用户状态
     * 仅更新非管理员角色的用户
     *
     * @param ids 用户ID列表
     * @param status 新状态
     * @return 实际更新的用户数量
     */
    @Transactional
    public int batchUpdateUserStatus(List<Integer> ids, String status) {
        log.debug("批量更新用户状态, IDs: {}, status: {}", ids, status);
        int updatedCount = userRepository.batchUpdateStatus(ids, status);
        log.info("批量更新用户状态完成, 更新数量: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 批量更新用户角色
     * 仅更新非管理员角色的用户
     *
     * @param ids 用户ID列表
     * @param role 新角色
     * @return 实际更新的用户数量
     */
    @Transactional
    public int batchUpdateUserRole(List<Integer> ids, String role) {
        log.debug("批量更新用户角色, IDs: {}, role: {}", ids, role);
        int updatedCount = userRepository.batchUpdateRole(ids, role);
        log.info("批量更新用户角色完成, 更新数量: {}", updatedCount);
        return updatedCount;
    }
}